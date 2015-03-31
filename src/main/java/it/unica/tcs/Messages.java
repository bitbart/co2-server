package it.unica.tcs;

/** Supports communication between client and server. 1) Collects all messages that server uses to respond at client
 * requests. 2) Provides methods to build xml custom responses with one or more messages. */
public class Messages {

	/* Possible types of response */
	public static final String TYPE_AUTH_ERROR = "auth_error";
	public static final String TYPE_GENERIC_ERROR = "error";
	public static final String TYPE_SUCCESS = "success";
	public static final String TYPE_CONTRACT_HASH = "contract_hash";
	public static final String TYPE_CONTRACT_REJECTED = "contract_rejected";
	public static final String TYPE_NO = "no";
	public static final String TYPE_YES = "yes";

	/* Contract properties messages */
	public static final String CONTRACT_VALID = "Contract submitted is a valid contract";
	public static final String CONTRACT_INVALID = "Contract submitted is not a valid contract";
	public static final String CONTRACT_REGISTERED = "Contract successfully registered";
	public static final String CONTRACT_NULL = "Not null contract required";
	public static final String CONTRACT_EMPTY = "Not empty contract required";
	public static final String CONTRACT_SAME_CONTEXT = "The two contracts must have the same context";
	public static final String CONTRACT_ACTION_CONTEXT = "Action not allowed in this context";
	public static final String CONTRACT_ADMITS_COMPLIANT = "Contract submitted admits a compliant";
	public static final String CONTRACT_DOESNT_ADMITS_COMPLIANT = "Contract submitted doesn't admits a compliant";
	public static final String CONTRACT_BUSY = "Contract requested is busy: it can't be merged";
	public static final String CONTRACT_NOT_PUBLISHED = "Contract cannot be registered in the middleware";

	/* Contract states messages */
	public final static String CONTRACT_LATENT_MESSAGE = "Contract is not fused yet";
	public final static String CONTRACT_FUSED_MESSAGE = "Contract is fused";
	public final static String CONTRACT_COMPLETED_MESSAGE = "Contract is consumed";
	public final static String CONTRACT_STUCK_MESSAGE = "Contract is stuck";

	/* Session messages */
	public static final String SESSION_COMPLIANT_NO = "Compliant contract not found yet";
	public static final String SESSION_COMPLIANT_YES = "Compliant contract found, session started";
	public static final String SESSION_ACTION_DENIED = "Action cannot be performed";
	public static final String SESSION_ACTION_DONE = "Action performed";
	public static final String SESSION_ACTION_NOT_PERFORMED = "Action is not been performed";
	public static final String SESSION_MOVE_BEFORE_START = "Can't move after the contract is started";
	public static final String SESSION_MOVE_AFTER_END = "Can't move after the contract is ended";

	/* Authentication messages */
	public static final String AUTH_FAILED = "Invalid username or password";
	public static final String PERMISSION_DENIED = "Permission denied";

	/* Context messages */
	public static final String CONTEXT_ERROR = "Invalid context";

	/* Error messages */
	public static final String ERROR_GENERIC_INTERNAL = "Service not available";
	public static final String ERROR_NO_SUCH_ALGORITHM = "Service not available";
	public static final String ERROR_TIME_EXPIRED = "Time expired";
	public static final String ERROR_TRANSLATION = "Error during translation";
	public static final String ERROR_VALIDATION = "Error during validation";
	public static final String ERROR_XML_PARSING = "Service not available";

	/* Database messages */
	public static final String DB_CONN_FAILED = "Service not available";
	public static final String DB_INSERT_FAILED = "Failed saving data";
	public static final String DB_SELECT_FAILED = "Failed loading data";

	/* Generic messages properties */
	public static final String PROPERTY_YES = "Property is satisfied";
	public static final String PROPERTY_NO = "Property is not satisfied";

	/** Build a response message.
	 * 
	 * @param type Type of response (i.e. error, success ...)
	 * @param message Real message to send
	 * @return Xml string to send to client */
	public static String printMessage(String type, String message) {

		String[] type_array, msg_array;

		type_array = new String[1];
		msg_array = new String[1];

		type_array[0] = type;
		msg_array[0] = message;

		return printMessage(type_array, msg_array);
	}

	/** Build a response message.
	 * 
	 * @param type Multiple type of response (i.e. error, success ...)
	 * @param messages Multiple messages to insert in the xml response
	 * @return Xml string to send to client */
	public static String printMessage(String[] types, String[] messages) {

		String tmp = new String("<xml>");

		if (types.length != messages.length) {
			Log.message().severe("Error passing types and messages to printMessage(): different array sizes!");
			return "<response type=\"error\">Internal error.</response>";
		}

		for (int i = 0; i < types.length; i++)
			tmp += "<response type=\"" + types[i] + "\">" + messages[i] + "</response>";

		return tmp + "</xml>";
	}
}
