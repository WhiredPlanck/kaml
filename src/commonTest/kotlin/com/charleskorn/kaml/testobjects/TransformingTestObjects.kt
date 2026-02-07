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

package com.charleskorn.kaml.testobjects

import com.charleskorn.kaml.YamlList
import com.charleskorn.kaml.YamlMap
import com.charleskorn.kaml.YamlNode
import com.charleskorn.kaml.YamlScalar
import com.charleskorn.kaml.YamlTransformingSerializer
import com.charleskorn.kaml.yamlList
import com.charleskorn.kaml.yamlMap
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KeepGeneratedSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.serializer

object UppercaseTransformingSerializer : YamlTransformingSerializer<String>(String.serializer()) {
    override fun transformSerialize(node: YamlNode): YamlNode = when (node) {
        is YamlScalar -> YamlScalar(node.content.uppercase(), node.path)
        else -> node
    }

    override fun transformDeserialize(node: YamlNode): YamlNode = when (node) {
        is YamlScalar -> YamlScalar(node.content.uppercase(), node.path)
        else -> node
    }
}

@Serializable
data class UppercasedValueSimpleStructure(
    @Serializable(with = UppercaseTransformingSerializer::class)
    val name: String,
)

object ShapeWrappingSerializer : YamlTransformingSerializer<Shapes>(Shapes.generatedSerializer()) {
    override fun transformSerialize(node: YamlNode): YamlNode {
        require(node is YamlMap)

        val firstNode = checkNotNull(node["first"]?.yamlMap)
        val restNode = checkNotNull(node["rest"]?.yamlList)

        val combined = listOf(firstNode) + restNode
        return YamlList(combined, node.path)
    }
}

@Serializable(with = ShapeWrappingSerializer::class)
@OptIn(ExperimentalSerializationApi::class)
@KeepGeneratedSerializer
data class Shapes(
    val first: Shape,
    val rest: List<Shape>,
)

@Serializable
sealed interface Shape {
    @Serializable
    data class Circle(val diameter: Int) : Shape

    @Serializable
    data class Rectangle(val a: Int, val b: Int) : Shape
}
