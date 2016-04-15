package it.unica.tcs;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class InternalException extends Exception {

    private static final Logger logger = LoggerFactory.getLogger(InternalException.class);
    
	public static enum ErrorTypes {
		TYPE_UNKNOWN, TYPE_NULL_CONTRACT, TYPE_EMPTY_CONTRACT, TYPE_CONTEXT_ERROR, TYPE_PERMISSION_DENIED, TYPE_TOO_ONDUTY, TYPE_ACTION_CULPABLE
	};

	private static final long serialVersionUID = -798397647243549412L;
	private String outputMessage;
	private Integer outputType;

	public InternalException(ErrorTypes type) {

		switch (type) {

			case TYPE_NULL_CONTRACT:
				outputType = 0;
				outputMessage = Messages.CONTRACT_INVALID + " ("+ Messages.CONTRACT_NULL + ")";
				break;

			case TYPE_EMPTY_CONTRACT:
				outputType = 0;
				outputMessage = Messages.CONTRACT_INVALID + " (" + Messages.CONTRACT_EMPTY + ")";
				break;

			case TYPE_CONTEXT_ERROR:
				outputType = -1;
				outputMessage =  Messages.CONTRACT_INVALID + " (" + Messages.CONTEXT_ERROR + ")";
				break;
				
			case TYPE_PERMISSION_DENIED:
				outputType = -1;
				outputMessage = Messages.PERMISSION_DENIED;
				break;
				
			case TYPE_TOO_ONDUTY:
				outputType= -1;
				outputMessage = Messages.ERROR_GENERIC_INTERNAL;
				logger.error("Two participants found on duty in the same session!");
				break;
				
			case TYPE_ACTION_CULPABLE:
				outputType = 2;
				outputMessage = Messages.ERROR_GENERIC_INTERNAL;
				logger.trace("The participant has executed 'the culpable action': now he is culpable.");
				
			default:
				outputType = -1;
				outputMessage = Messages.ERROR_GENERIC_INTERNAL;
		}
	}

	@Override
	public String getMessage() {

		return outputMessage;
	}

	public Integer getType() {

		return outputType;
	}
}
