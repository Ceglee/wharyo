CREATE TYPE layer_type AS ENUM ('DATABASE', 'SHAPEFILE')
CREATE TABLE layer_config (
	id INTEGER PRIMARY KEY,
	layer_name VARCHAR(255) NOT NULL UNIQUE,
	layer_type layer_type NOT NULL
)