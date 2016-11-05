package pl.wharyo.exceptions;

public class LayerDataSourceNotAvailableException extends Exception {
	private Reason reason;
	
	public LayerDataSourceNotAvailableException(String msg, Reason reason) {
		super(msg);
		this.reason = reason;
	}
	public Reason getReason() {
		return reason;
	}
	public static enum Reason {
		CONNECTION_UNAVAILABLE, READONLY_ACCESS
	}
}
