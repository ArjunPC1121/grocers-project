# Products App

Spring Boot product API with hybrid search: Oracle Text provides lexical search and the application performs semantic ranking from locally generated embeddings.

## Before first run

1. Install and start Oracle Free Database. The default connection used by this project is `localhost:1521/free`.
2. Install JDK 25 and Maven.
3. Update the database connection values in `src/main/resources/application.properties`:

   ```properties
   spring.datasource.username=system
   spring.datasource.password=your_password
   ```

4. Keep `spring.jpa.hibernate.ddl-auto=update`. Hibernate creates the `PRODUCT` table if it does not exist and preserves existing data.

## First run

Run the application once. This creates the `PRODUCT` table:

```powershell
Set-Location 'C:\path\to\productsapp'

$env:JAVA_HOME = 'C:\Program Files\Java\jdk-25.0.4'
$env:Path = "$env:JAVA_HOME\bin;$env:Path"

mvn spring-boot:run '-Dspring-boot.run.jvmArguments=--enable-native-access=ALL-UNNAMED'
```

Stop the application after it starts, then connect to Oracle as `SYSTEM` and create the Oracle Text index once:

```sql
CREATE INDEX product_search_text_idx
ON product (search_text)
INDEXTYPE IS CTXSYS.CONTEXT
PARAMETERS ('SYNC (ON COMMIT)');
```

## After setup

Start the application with the same PowerShell command. The API is available at:

```text
http://localhost:8083/api/products
```

The hybrid search endpoint is:

```text
GET http://localhost:8083/api/products/search?q=milk&limit=10
```

## Important notes

- `TEXT_EMBEDDING` is stored as `RAW(1536)` so the table can remain in Oracle's `SYSTEM` tablespace; Oracle `VECTOR` columns are not used.
- `spring.sql.init.mode=never` prevents repeated index creation when the application restarts.
- Do not use `ddl-auto=create` unless you intend to delete and recreate the product table.
