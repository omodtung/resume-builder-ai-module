package com.rag.AiResume.model;

import java.io.Serializable;

public class ChatResponse  implements Serializable {
    
	private String responseId;
	private String response;

	public String getResponseId() {
		return responseId;
	}

	public void setResponseId(String responseId) {
		this.responseId = responseId;
	}

	public String getResponse() {
		return response;
	}

	public void setResponse(String response) {
		this.response = response;
	}
}
