package guideme.volunteers.helpers.dataservices.errors

import java.io.Serializable

class ErrorMessage(val errorType : ErrorType, val content : String? = null) : Serializable
