package me.bristermitten.mittenlib.annotations.compile

import com.google.inject.Inject
import me.bristermitten.mittenlib.annotations.ast.Property
import me.bristermitten.mittenlib.config.names.ConfigName
import me.bristermitten.mittenlib.config.names.NamingPattern
import me.bristermitten.mittenlib.config.names.NamingPatternTransformer
import org.jspecify.annotations.Nullable

class FieldNameGenerator @Inject() ():

  def getConfigFieldName(property: Property): String =
    val configName = property.settings().configName()
    val namingPattern = property.settings().namingPattern()
    val fieldName = property.name()

    getConfigFieldName(configName, namingPattern, fieldName)

  private def getConfigFieldName(
      @Nullable configName: ConfigName,
      @Nullable namingPattern: NamingPattern,
      fieldName: String
  ): String =
    if (configName != null) {
      configName.value()
    } else if (namingPattern != null) {
      NamingPatternTransformer.format(fieldName, namingPattern.value())
    } else {
      fieldName
    }
