import { ApolloServer } from '@apollo/server';
import { startStandaloneServer } from '@apollo/server/standalone';
import { RestClient } from './datasources/restClient.js';
import { createResolvers } from './resolvers/index.js';
import { typeDefs } from './schema/typeDefs.js';

const port = Number(process.env.PORT || 4000);
const restBaseUrl = process.env.REST_BASE_URL || 'http://localhost:8080';

const rest = new RestClient(restBaseUrl);
const server = new ApolloServer({
  typeDefs,
  resolvers: createResolvers(rest),
});

const { url } = await startStandaloneServer(server, {
  listen: { port },
});

console.log(`GraphQL BFF ready at ${url} (REST upstream: ${restBaseUrl})`);
