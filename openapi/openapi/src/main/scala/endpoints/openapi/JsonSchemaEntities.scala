package endpoints
package openapi

import endpoints.algebra
import endpoints.algebra.Documentation
import endpoints.openapi.model.{MediaType, Schema}

/**
  * Interpreter for [[algebra.JsonSchemaEntities]] that produces a documentation of the JSON schemas.
  */
trait JsonSchemaEntities
  extends algebra.JsonSchemaEntities
    with Endpoints
    with JsonSchemas {

  def jsonRequest[A](docs: Documentation)(implicit codec: JsonSchema[A]): Option[DocumentedRequestEntity] =
    Some(DocumentedRequestEntity(docs, Map("application/json" -> MediaType(Some(toSchema(codec))))))

  def jsonResponse[A](docs: Documentation)(implicit codec: JsonSchema[A]): List[DocumentedResponse] =
    DocumentedResponse(200, docs.getOrElse(""), Map("application/json" -> MediaType(Some(toSchema(codec))))) :: Nil

  def toSchema(documentedCodec: DocumentedJsonSchema): Schema = {
    import DocumentedJsonSchema._
    documentedCodec match {
      case DocumentedRecord(fields) =>
        val fieldsSchema =
          fields.map(f => Schema.Property(f.name, toSchema(f.tpe), !f.isOptional, f.documentation))
        Schema.Object(fieldsSchema, None)
      case DocumentedCoProd(alternatives) =>
        val alternativesSchemas =
          alternatives.map { case (tag, record) =>
            Schema.Object(Schema.Property(tag, toSchema(record), isRequired = true, description = None) :: Nil, None)
          }
        Schema.OneOf(alternativesSchemas, None)
      case Primitive(name) => Schema.Primitive(name)
      case Array(elementType) => Schema.Array(toSchema(elementType))
    }
  }

}
