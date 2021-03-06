/*
 * Copyright 2011 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.modelmapper.internal;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.modelmapper.Condition;
import org.modelmapper.Converter;
import org.modelmapper.PropertyMap;
import org.modelmapper.Provider;
import org.modelmapper.TypeMap;
import org.modelmapper.config.Configuration;
import org.modelmapper.internal.util.Assert;
import org.modelmapper.internal.util.Types;
import org.modelmapper.spi.Mapping;
import org.modelmapper.spi.PropertyInfo;

/**
 * TypeMap implementation.
 * 
 * @author Jonathan Halterman
 */
class TypeMapImpl<S, D> implements TypeMap<S, D> {
  private final Class<S> sourceType;
  private final Class<D> destinationType;
  final Configuration configuration;
  private final MappingEngineImpl engine;
  /** Guarded by "mappings" */
  private final Map<String, PropertyInfo> mappedProperties = new HashMap<String, PropertyInfo>();
  /** Guarded by "mappings" */
  private final Map<String, MappingImpl> mappings = new TreeMap<String, MappingImpl>();
  private Converter<S, D> converter;
  private Condition<?, ?> condition;
  private Provider<D> provider;

  TypeMapImpl(Class<S> sourceType, Class<D> destinationType, Configuration configuration,
      MappingEngineImpl engine) {
    this.sourceType = sourceType;
    this.destinationType = destinationType;
    this.configuration = configuration;
    this.engine = engine;
  }

  public void addMappings(PropertyMap<S, D> propertyMap) {
    synchronized (mappings) {
      for (MappingImpl mapping : new MappingBuilderImpl<S, D>(sourceType, destinationType,
          configuration).build(propertyMap)) {
        MappingImpl existingMapping = addMapping(mapping);
        if (existingMapping != null && existingMapping.isExplicit())
          new Errors().duplicateMapping(mapping.getLastDestinationProperty())
              .throwConfigurationExceptionIfErrorsExist();
      }
    }
  }

  public Condition<?, ?> getCondition() {
    return condition;
  }

  public Converter<S, D> getConverter() {
    return converter;
  }

  public Class<D> getDestinationType() {
    return destinationType;
  }

  public List<Mapping> getMappings() {
    synchronized (mappings) {
      return new ArrayList<Mapping>(mappings.values());
    }
  }

  public Provider<D> getProvider() {
    return provider;
  }

  public Class<S> getSourceType() {
    return sourceType;
  }

  public List<PropertyInfo> getUnmappedProperties() {
    TypeInfo<D> destinationInfo = TypeInfoRegistry.typeInfoFor(destinationType, configuration);
    List<PropertyInfo> unmapped = new ArrayList<PropertyInfo>();

    synchronized (mappings) {
      for (Map.Entry<String, Mutator> entry : destinationInfo.getMutators().entrySet())
        if (!mappedProperties.containsKey(entry.getKey()))
          unmapped.add(entry.getValue());
    }

    return unmapped;
  }

  public D map(S source) {
    Class<S> sourceType = Types.<S>deProxy(source.getClass());
    MappingContextImpl<S, D> context = new MappingContextImpl<S, D>(source, sourceType, null,
        destinationType, engine);
    D result = null;

    try {
      result = engine.typeMap(context, this);
    } catch (Throwable t) {
      context.errors.errorMapping(sourceType, destinationType, t);
    }

    context.errors.throwMappingExceptionIfErrorsExist();
    return result;
  }

  public void map(S source, D destination) {
    Class<S> sourceType = Types.<S>deProxy(source.getClass());
    MappingContextImpl<S, D> context = new MappingContextImpl<S, D>(source, sourceType,
        destination, destinationType, engine);

    try {
      engine.typeMap(context, this);
    } catch (Throwable t) {
      context.errors.errorMapping(sourceType, destinationType, t);
    }

    context.errors.throwMappingExceptionIfErrorsExist();
  }

  public TypeMap<S, D> setCondition(Condition<?, ?> condition) {
    this.condition = Assert.notNull(condition, "condition");
    return this;
  }

  public TypeMap<S, D> setConverter(Converter<S, D> converter) {
    this.converter = Assert.notNull(converter, "converter");
    return this;
  }

  public TypeMap<S, D> setProvider(Provider<D> provider) {
    this.provider = Assert.notNull(provider, "provider");
    return this;
  }

  @Override
  public String toString() {
    return String.format("TypeMap[%s -> %s]", Types.toString(sourceType),
        Types.toString(destinationType));
  }

  public void validate() {
    if (converter != null)
      return;

    Errors errors = new Errors();
    List<PropertyInfo> unmappedProperties = getUnmappedProperties();
    if (!unmappedProperties.isEmpty())
      errors.errorUnmappedProperties(this, unmappedProperties);

    errors.throwValidationExceptionIfErrorsExist();
  }

  MappingImpl addMapping(MappingImpl mapping) {
    synchronized (mappings) {
      mappedProperties.put(mapping.getDestinationProperties().get(0).getName(), mapping
          .getDestinationProperties().get(0));
      return mappings.put(mapping.getPath(), mapping);
    }
  }

  /**
   * Used by PropertyMapBuilder to determine if a mapping for the {@code path} already exists. No
   * need to synchronize here since the TypeMap is not exposed publicly yet.
   */
  boolean isMapped(String path) {
    return mappedProperties.containsKey(path);
  }
}
