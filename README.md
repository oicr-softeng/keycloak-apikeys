# keycloak-apikeys
Keycloak plugin to enable API Keys for user authentication.

This extension extends Keycloak Server providing additional REST endpoints to create and manage API Keys.

## Build
`mvn clean package`

## Docker setup
1. Build using `mvn clean package`
2. Run `docker-compose up`

## Endpoints 

### List API Keys
- **URL**: http://localhost/realms/{realm_name}/apikey/api_key
- **Method**: `GET`
- **Authorization**: `Bearer token` of the owner of the apiKey.
- **Description**: Retrieves a list of API Keys by user.
- **Query Params**:  
  - `user_id` (string) - The ID of the user.
  - `query` (string) optional - The ID of an API Key to filter.
  - `limit` (string) optional - Allows to limit the number of API Keys to retrieve. Default *20*.
  - `offset` (string) optional - Allows to omit a specified number of API Keys before the beginning of the result set. Default *0*.
  - `sort` (string) optional - Sort API Keys by a specific field. Values: *expirydate, issuedate, isrevoked, description*. Default *name*.
  - `sortOrder` (string) optional - Ascending or descending order. Values: *ASC* or *DESC*. Default *DESC*.

### Create API Key
- **URL**: http://localhost/realms/{realm_name}/apikey/api_key
- **Method**: `POST`
- **Authorization**: `Bearer token` of the owner of the apiKey or an Admin user.
- **Description**: Creates a new API Key.
- **Query Params**:
    - `user_id` (string) - The ID of the user.
    - `description` (string) - Description of the API Key.
    - `scopes` (string) 1 or more - A permission of this API Key format *{policy}.{access_level}*.

### Check API Key
- **URL**: http://localhost/realms/{realm_name}/apikey/check_api_key
- **Method**: `POST`
- **Authorization**: `Bearer token` of the owner of the apiKey or `Basic auth` of a client.
- **Description**: Checks the API Key.
- **Query Params**:
    - `apiKey` (string) - The ID of an API Key

### Revoke an API Key
- **URL**: http://localhost/realms/{realm_name}/apikey/api_key
- **Method**: `DELETE`
- **Authorization**: `Bearer token` of the owner of the apiKey or an Admin user.
- **Description**: Revoke a specific API Key.
- **Query Params**:
    - `apiKey` (string) - The ID of an API Key.


