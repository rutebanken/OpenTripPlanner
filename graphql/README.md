This folder contain a GraphQL Endpoints scratch book. OTP have 2 enpoints,
the Transmodel and Gtfs - for witch there is project catalogs for here.

Intellij have tools to work with GraphQL, but first you should install the 
cli graphql tool: https://github.com/graphql-cli/graphql-cli

The _.graphqlconfig.yaml_ file contain the configuration - the cli tool read this file as well as IntelliJ, be sure 
to be in the project root folder when running the cli tool.

The `gtfs` and `transmodel` folder contain:

- _schema.graphql_ the generated schema from the endpoint. Created with `$ graphql get-schema`. Note that the schema 
  file is generated.
- _queryA.graphql_ Example query A.
- _queryB.graphql_ Example query B.
- and so on...

You may use Intellij to run queries. 
1. Start OTP
2. Select a query with the editor cursor
3. Press `Command ⌘` + `Enter ↩` or click the green run button above your window.

