/**
 * Copyright (C) 2010-2011 LShift Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.lshift.diffa.docgen;

import javax.ws.rs.*;
import javax.ws.rs.core.Response;

import net.lshift.diffa.docgen.annotations.*;
import net.lshift.diffa.docgen.annotations.MandatoryParams.MandatoryParam;
import net.lshift.diffa.docgen.annotations.OptionalParams.OptionalParam;
import org.antlr.stringtemplate.StringTemplate;
import org.antlr.stringtemplate.StringTemplateGroup;
import org.codehaus.jackson.map.ObjectMapper;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RestGen {
    private static final ObjectMapper mapper = new ObjectMapper();
    private final StringTemplateGroup group;
    private final Map<String, String> resultFiles = new HashMap<String, String>();
    private final List<ResourceCollection> collections = new ArrayList<ResourceCollection>();
    private static final String BASE_PATH = "/doc/rest";
    private final Map<Class,Path> pathIndex = new HashMap<Class,Path>();

    //mapper.getSerializationConfig().set(SerializationConfig.Feature.INDENT_OUTPUT, true);
    //mapper.getSerializationConfig().set(SerializationConfig.Feature.WRAP_ROOT_VALUE, true);
    public RestGen(String templateDir, Class parentResource) {
        this.group = new StringTemplateGroup("rest", templateDir);

        for(Method m : parentResource.getMethods()) {
          Path path = m.getAnnotation(Path.class);
          if (path != null) {
            pathIndex.put(m.getReturnType(),path);
          }
        }
    }

    public void addResources(Class<?> clazz, Map<Class<?>, Object> examples)
            throws Exception {
        collections.add(generateCollection(pathIndex, clazz, examples));
    }


    public Map<String, String> getFiles() {
        // Deactivate all resources
        for (ResourceCollection collection : collections)
            collection.setActiveResource(null);

        // Generate pages for resources
        for (ResourceCollection collection : collections) {
            for (ResourceDescriptor resource : collection.getResources()) {
                collection.setActiveResource(resource);

                StringTemplate template = group.getInstanceOf("rest_resource");
                template.setAttribute("collections", collections);
                template.setAttribute("collection", collection);
                template.setAttribute("resource", resource);
                template.setAttribute("basePath", BASE_PATH);
                template.setPassThroughAttributes(true);

                resultFiles.put(resource.getDocPath() + "/index.md", template.toString());
            }
            collection.setActiveResource(null);
        }

        // Generate index
        resultFiles.put("index.md", genIndex());
        return resultFiles;
    }

    private String genIndex() {
        StringTemplate template = group.getInstanceOf("rest_collections");

        template.setAttribute("collections", collections);
        return template.toString();
    }

    private static void jsonify(ResourceDescriptor rd, Class<?> t, Map<Class<?>, Object> examples)
            throws Exception {
        Class<?> classToMap = t.isArray() ? t.getComponentType() : t;

        rd.setEntityName(t.isArray() ? arrayize(classToMap.getSimpleName()) : classToMap.getSimpleName());

        if (examples.containsKey(classToMap)) {
            Object exampleObj = t.isArray() ? new Object[] { examples.get(classToMap) } : examples.get(classToMap);
            String example = mapper.defaultPrettyPrintingWriter().writeValueAsString(exampleObj);
            rd.setExample(example);
        }
    }

    private static String arrayize(String type) {
        return "Array of " + type;
    }

    private static <T> ResourceCollection generateCollection(Map<Class,Path> pathIndex, Class<T> clazz, Map<Class<?>, Object> examples)
            throws Exception {
        boolean domain = true;

        // For domain bound resources, the path is annotated on the parent resource
        Path path = pathIndex.get(clazz);

        // For non-domain resources, the path is annotated on itself
        if (path == null) {
          path = clazz.getAnnotation(Path.class);
          domain = false;
        }


        List<ResourceDescriptor> resources = new java.util.ArrayList<ResourceDescriptor>();

        for (Method x : clazz.getMethods()) {
            Path p = x.getAnnotation(Path.class);
            if (p != null) {
                ResourceDescriptor rd = new ResourceDescriptor();
                rd.setCollectionPath(path.value());
                rd.setDomain(domain);

                if (x.isAnnotationPresent(GET.class))
                    rd.setMethod("GET");

                if (x.isAnnotationPresent(PUT.class))
                    rd.setMethod("PUT");

                if (x.isAnnotationPresent(POST.class))
                    rd.setMethod("POST");

                if (x.isAnnotationPresent(DELETE.class))
                    rd.setMethod("DELETE");

                if (x.isAnnotationPresent(Description.class))
                    rd.setDescription(x.getAnnotation(Description.class).value());

                // Add mandatory URL parameters
                if (x.isAnnotationPresent(MandatoryParams.class)) {
                    for (MandatoryParam param : x.getAnnotation(MandatoryParams.class).value())
                        rd.getMandatoryParameters().add(
                                new MandatoryParameter(param.name(), param.datatype(), param.description()));
                }

                // Add optional query parameters
                if (x.isAnnotationPresent(OptionalParams.class)) {
                    for (OptionalParam param : x.getAnnotation(OptionalParams.class).value()) {
                        rd.getOptionalParameters().add(
                                new OptionalParameter(param.name(), param.datatype(), param.description()));
                    }
                }
                
                Class<?> rt = x.getReturnType();
                if (!rt.getName().equals("void") && !rt.equals(Response.class)) {
                    jsonify(rd, rt, examples);
                }

                for (Class<?> y : x.getParameterTypes()) {
                    if (!y.equals(String.class))
                        jsonify(rd, rt, examples);
                }

                rd.setPath(p.value());

                resources.add(rd);
            }

        }

        return new ResourceCollection(clazz.getName(), path.value(), resources, domain);
    }
}