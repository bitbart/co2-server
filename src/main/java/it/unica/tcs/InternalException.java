package it.unica.tcs;

import it.unica.tcs.Messages;

public class InternalException extends Exception {

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
				Log.message().severe("Two participants found on duty in the same session!");
				break;
				
			case TYPE_ACTION_CULPABLE:
				outputType = 2;
				outputMessage = Messages.ERROR_GENERIC_INTERNAL;
				Log.message().fine("The participant has executed 'the culpable action': now he is culpable.");
				
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
