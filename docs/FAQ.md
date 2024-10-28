<p align="center">
<img src="./cdpg.png" width="300">
</p>

# Frequently Asked Questions (FAQs)

1. How do I request for a new feature to be added or change in an existing feature?
- Please create an issue [here](https://github.com/datakaveri/iudx-resource-server/issues)
2. What do we do when there is any error during flyway migration?
- We could run this command `mvn flyway:repair` and do the flyway migration again
-If the error persists, it needs to be resolved manually and a backup of the database could be taken from postgres if the table needs to be changed

3. “Request could not be created, as resource was not found” - even if the resource is found while creating access request
- This error occurs when the resource server URL that the consumer is associated to while requesting the API, does not match with the resource server URL of the resource item

4. What types of search functionalities does the DX Resource Server support?
- The server supports:
    - Spatial Search: Search using Circle, Polygon, Bounding Box (Bbox), and Linestring.
    - Temporal Search: Search based on time, including Before, During, and After criteria.
    - Attribute Search: Search based on specific resource attributes.
    - Latest Search: Give the packet of latest data available on the server.
    - Complex Search: Combination of Spatial Search, Temporal Search, Attribute Search
      
5. Does the DX Resource Server support encrypted data access?
- Yes, the DX Resource Server supports encrypted data access.