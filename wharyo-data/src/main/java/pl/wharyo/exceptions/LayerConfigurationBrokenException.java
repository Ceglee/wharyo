package pl.wharyo.exceptions;

public class LayerConfigurationBrokenException extends Exception {

	private Reason reason;
	
	public LayerConfigurationBrokenException(String msg, Reason reason) {
		super(msg);
		this.reason = reason;
	}
	
	public Reason getReason() {
		return this.reason;
	}
	
	public static enum Reason {
		INVALID_ID_FIELD, NO_ATTRIBUTE_METADATA, NO_GEOMETRY_METADATA
	}
}
