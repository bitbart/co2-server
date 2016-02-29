package it.unica.tcs;


public class ResponsePacket {
	
	public static final Integer TYPE_YES = 1;
	public static final Integer TYPE_NO = 0;
	public static final Integer TYPE_ERROR = -1;

	private Integer type;
	private String content;
	private String hash;
	private String actionName;
	private String actionValue;

	// Must have no-argument constructor
	public ResponsePacket() {

	}

	public ResponsePacket(Integer type, String content) {
		this.type = type;
		this.content = content;
	}
	
	public ResponsePacket(Integer type, String content, String hash) {
		this.type = type;
		this.content = content;
		this.hash = hash;
	}
	
	public Integer getType() {
		
		return type;
	}
	
	public String getContent() {
		
		return content;
	}
	
	public String getHash() {
		
		return hash;
	}
	
	public String getActionName() {
		return actionName;
	}
	
	public String getActionValue() {
		return actionValue;
	}
	
	public void setType(Integer type) {
		this.type = type;
	}
	
	public void setContent(String content) {
		this.content = content;
	}
	
	public void setHash(String hash) {
		this.hash = hash;
	}
	
	public void setActionName(String actionName) {
		this.actionName = actionName;
	}
	
	public void setActionValue(String actionValue) {
		this.actionValue = actionValue;
	}

}