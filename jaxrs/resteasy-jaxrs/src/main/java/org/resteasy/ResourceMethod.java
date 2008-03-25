package org.resteasy;

import org.resteasy.spi.HttpRequest;
import org.resteasy.spi.HttpResponse;
import org.resteasy.spi.MethodInjector;
import org.resteasy.spi.ResourceFactory;
import org.resteasy.spi.ResteasyProviderFactory;

import javax.ws.rs.ConsumeMime;
import javax.ws.rs.ProduceMime;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Set;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class ResourceMethod
{

   protected MediaType[] produces;
   protected MediaType[] consumes;
   protected Set<String> httpMethods;
   protected MethodInjector injector;
   protected ResourceFactory factory;
   protected ResteasyProviderFactory providerFactory;
   protected PathParamIndex index;
   protected Method method;

   public ResourceMethod(Class<?> clazz, Method method, MethodInjector injector, ResourceFactory factory, ResteasyProviderFactory providerFactory, Set<String> httpMethods, PathParamIndex index)
   {
      this.injector = injector;
      this.factory = factory;
      this.providerFactory = providerFactory;
      this.httpMethods = httpMethods;
      this.index = index;
      this.method = method;

      ProduceMime p = method.getAnnotation(ProduceMime.class);
      if (p == null) p = clazz.getAnnotation(ProduceMime.class);
      ConsumeMime c = method.getAnnotation(ConsumeMime.class);
      if (c == null) c = clazz.getAnnotation(ConsumeMime.class);

      if (p != null)
      {
         produces = new MediaType[p.value().length];
         int i = 0;
         for (String mediaType : p.value())
         {
            produces[i++] = MediaType.parse(mediaType);
         }
      }
      if (c != null)
      {
         consumes = new MediaType[c.value().length];
         int i = 0;
         for (String mediaType : c.value())
         {
            consumes[i++] = MediaType.parse(mediaType);
         }
      }
   }

   public Method getMethod()
   {
      return method;
   }

   public Response invoke(HttpRequest request, HttpResponse response)
   {
      // we have to check if its a ResourceLocator because we don't want the template params
      // to be populated with wrong information.
      if (!(factory instanceof ResourceLocator)) index.populateUriInfoTemplateParams(request);
      Object target = factory.createResource(request, response);
      if (factory instanceof ResourceLocator) index.populateUriInfoTemplateParams(request);
      return injector.invoke(request, response, target);
   }


   public boolean doesProduce(List<MediaType> accepts)
   {
      if (accepts == null || accepts.size() == 0)
      {
         //System.out.println("**** no accepts " +" method: " + method);
         return true;
      }
      if (produces == null || produces.length == 0)
      {
         //System.out.println("**** no produces " +" method: " + method);
         return true;
      }

      for (MediaType accept : accepts)
      {
         for (MediaType type : produces)
         {
            if (type.isCompatible(accept))
            {
               return true;
            }
         }
      }
      return false;
   }

   public boolean doesConsume(MediaType contentType)
   {
      boolean matches = false;
      if (contentType == null)
      {
         matches = true;
      }
      else
      {
         if (consumes == null || consumes.length == 0)
         {
            matches = true;
         }
         else
         {
            for (MediaType type : consumes)
            {
               if (type.isCompatible(contentType))
               {
                  matches = true;
                  break;
               }
            }
         }
      }
      return matches;
   }

   public MediaType matchByType(List<MediaType> accepts)
   {
      if (accepts == null || accepts.size() == 0)
      {
         if (produces == null) return MediaType.parse("*/*");
         else return produces[0];
      }

      if (produces == null || produces.length == 0) return accepts.get(0);

      for (MediaType accept : accepts)
      {
         for (MediaType type : produces)
         {
            if (type.isCompatible(accept)) return type;
         }
      }
      return null;
   }

   public Set<String> getHttpMethods()
   {
      return httpMethods;
   }

   public MediaType[] getProduces()
   {
      return produces;
   }

   public MediaType[] getConsumes()
   {
      return consumes;
   }
}
