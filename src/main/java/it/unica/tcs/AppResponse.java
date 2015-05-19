package it.unica.tcs;

public class AppResponse {

		private String appOutput;
		private String appError;
		private boolean hasErrors;
		private boolean emptyOutput;
		
		public AppResponse() {
			
			this.appOutput = "";
			this.appError = "";
			this.hasErrors = false;
			this.emptyOutput = true;
		}
		
		public AppResponse(String appOutput, String appError) {
			
			this.appOutput = appOutput;
			this.appError = appError;
			
			if (appError.equals(""))
				hasErrors = false;
			else
				hasErrors = true;
			
			if (appOutput.equals(""))
				emptyOutput = true;
			else
				emptyOutput = false;
		}
		
		public String getOutput() {
			
			return appOutput;
		}
		
		public boolean isEmpty() {
			
			return emptyOutput;
		}
		
		public String getErrors() {
			
			return appError;
		}
		
		public boolean hasErrors() {
			
			return hasErrors;
		}
		
		public void setOutput(String output) {
			
			this.appOutput = output;
			
			if (appOutput.equals(""))
				emptyOutput = true;
			else
				emptyOutput = false;
		}
		
		public void setErrors(String errors) {
			
			this.appError = errors;
			
			if (appError.equals(""))
				hasErrors = false;
			else
				hasErrors = true;
		}
}
