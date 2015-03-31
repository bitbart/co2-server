package it.unica.tcs;


public class ResponsePacket {
	
	public static final Integer TYPE_YES = 1;
	public static final Integer TYPE_NO = 0;
	public static final Integer TYPE_ERROR = -1;

	private Integer type;
	private String content;

	// Must have no-argument constructor
	public ResponsePacket() {

	}

	public ResponsePacket(Integer type, String content) {
		this.type = type;
		this.content = content;
	}
	
	public Integer getType() {
		
		return type;
	}
	
	public String getContent() {
		
		return content;
	}
	
	public void setType(Integer type) {
		this.type = type;
	}
	
	public void setContent(String content) {
		this.content = content;
	}
}