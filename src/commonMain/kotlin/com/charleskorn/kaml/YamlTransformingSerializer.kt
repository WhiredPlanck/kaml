/*

   Copyright 2018-2023 Charles Korn.

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       https://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.

*/

package com.charleskorn.kaml

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

/**
 * Base class for custom serializers that allows manipulating an abstract JSON
 * representation of the class before serialization or deserialization.
 *
 * [YamlTransformingSerializer] provides capabilities to manipulate [YamlNode] representation
 * directly instead of interacting with [Encoder] and [Decoder] in order to apply a custom
 * transformation to the YAML.
 * Please note that this class expects that [Encoder] and [Decoder] are implemented by [YamlInput] and [YamlOutput],
 * i.e. serializers derived from this class work only with [Yaml] format.
 *
 * There are two methods in which YAML transformation can be defined: [transformSerialize] and [transformDeserialize].
 * You can override one or both of them. Consult their documentation for details.
 *
 * @param T A type for Kotlin property for which this serializer could be applied.
 *        **Not** the type that you may encounter in JSON. (e.g. if you unwrap a list
 *        to a single value `T`, use `T`, not `List<T>`)
 * @param tSerializer A serializer for type [T]. Determines [YamlNode] which is passed to [transformSerialize].
 *        Should be able to parse [YamlNode] from [transformDeserialize] function.
 *        Usually, default [serializer] is sufficient.
 */
public abstract class YamlTransformingSerializer<T : Any?>(
    private val tSerializer: KSerializer<T>,
) : KSerializer<T> {

    /**
     * A descriptor for this transformation.
     * By default, it delegates to [tSerializer]'s descriptor.
     *
     * However, this descriptor can be overridden to achieve better representation of the resulting YAML shape
     * for schema generating or introspection purposes.
     */
    override val descriptor: SerialDescriptor get() = tSerializer.descriptor

    final override fun serialize(encoder: Encoder, value: T) {
        val output = encoder.asYamlOutput()
        var node = output.yaml.encodeToYamlNode(tSerializer, value)
        node = transformSerialize(node)
        output.encodeSerializableValue(YamlNodeSerializer, node)
    }

    final override fun deserialize(decoder: Decoder): T {
        val input = decoder.asYamlInput<YamlInput>()
        return input.yaml.decodeFromYamlNode(tSerializer, transformDeserialize(input.node))
    }

    /**
     * Transformation that happens during [deserialize] call.
     * Does nothing by default.
     *
     * During deserialization, a value from YAML is firstly decoded to a [YamlNode],
     * user transformation in [transformDeserialize] is applied,
     * and then resulting [YamlNode] is deserialized to [T] with [tSerializer].
     */
    protected open fun transformDeserialize(node: YamlNode): YamlNode = node

    /**
     * Transformation that happens during [serialize] call.
     * Does nothing by default.
     *
     * During serialization, a value of type [T] is serialized with [tSerializer] to a [YamlNode],
     * user transformation in [transformSerialize] is applied, and then resulting [YamlNode] is encoded to a YAML string.
     */
    protected open fun transformSerialize(node: YamlNode): YamlNode = node
}
