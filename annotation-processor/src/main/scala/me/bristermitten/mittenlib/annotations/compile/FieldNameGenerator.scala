package me.bristermitten.mittenlib.annotations.compile

import com.google.inject.Inject
import me.bristermitten.mittenlib.annotations.domain.Property
import me.bristermitten.mittenlib.config.names.ConfigName
import me.bristermitten.mittenlib.config.names.NamingPattern
import me.bristermitten.mittenlib.config.names.NamingPatternTransformer
import org.jspecify.annotations.Nullable

class FieldNameGenerator @Inject() ():

  def getConfigFieldName(property: Property): String =
    val configName = property.configName.orNull
    val namingPattern = property.namingPattern.orNull
    val fieldName = property.name

    if (configName != null) {
      configName
    } else if (namingPattern != null) {
      NamingPatternTransformer.format(fieldName, namingPattern.value())
    } else {
      fieldName
    }

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
