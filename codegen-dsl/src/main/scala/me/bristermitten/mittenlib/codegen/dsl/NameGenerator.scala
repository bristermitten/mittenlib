package me.bristermitten.mittenlib.codegen.dsl

import scala.collection.mutable

/**
 * Generates unique variable names within a method body.
 *
 * Shared across all nested [[BlockBuilder]]s so names are unique
 * across the whole method, not just within each block.
 *
 * Given hint `"result"`, successive calls produce: `result`, `result1`, `result2`, …
 */
class NameGenerator:
  private val counters = mutable.Map.empty[String, Int]

  def generate(tpe: TypeRef, hint: Option[String] = None): Var =
    val base    = hint.filter(_.nonEmpty).getOrElse(deriveFromType(tpe))
    val clean   = sanitize(base)
    val count   = counters.getOrElse(clean, 0)
    counters(clean) = count + 1
    val name    = if count == 0 then clean else s"$clean$count"
    Var(name, tpe)

  private def deriveFromType(tpe: TypeRef): String =
    val raw = tpe match
      case TypeRef.Simple(t)       =>
        val s = t.toString
        val i = s.lastIndexOf('.')
        if i >= 0 then s.substring(i + 1) else s
      case TypeRef.Parameterized(r, _) => r.simpleName
      case TypeRef.ArrayOf(c)     => deriveFromType(c) + "Array"
    if raw.isEmpty then "v"
    else raw.head.toLower.toString + raw.tail

  private def sanitize(name: String): String =
    val sb = StringBuilder()
    name.zipWithIndex.foreach { (c, i) =>
      if i == 0 && !Character.isJavaIdentifierStart(c) then sb += '_'
      else if !Character.isJavaIdentifierPart(c) then sb += '_'
      else sb += c
    }
    if sb.isEmpty then "v" else sb.toString
