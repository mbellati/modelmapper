/**
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
package org.modelmapper.internal.converter;

import org.modelmapper.spi.ConditionalConverter;
import org.modelmapper.spi.MappingContext;

/**
 * Converts to {@link Character} instances.
 * 
 * @author Jonathan Halterman
 */
class CharacterConverter implements ConditionalConverter<Object, Character> {
  @Override
  public Character convert(MappingContext<Object, Character> context) {
    return new Character(context.getSource().toString().charAt(0));
  }

  @Override
  public boolean supports(Class<?> sourceType, Class<?> destinationType) {
    return destinationType == Character.class || destinationType == Character.TYPE;
  }

  @Override
  public boolean supportsSource(Class<?> sourceType) {
    return sourceType == Character.class || sourceType == Character.TYPE;
  }

  @Override
  public boolean verifiesSource() {
    return false;
  }
}