package pl.wharyo.dao;

import org.springframework.jdbc.core.JdbcTemplate;

public abstract class DAO {
	private JdbcTemplate template;
	
	public DAO(JdbcTemplate template) {
		this.template = template;
	}

	public JdbcTemplate getTemplate() {
		return template;
	}

	public void setTemplate(JdbcTemplate template) {
		this.template = template;
	}
	
}
