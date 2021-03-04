import storage from '@/components/shared/PersistentStorage';
// Default constant values
const QUERIES_LS_KEY = 'fav_queries';

export default {
  getFavQueries(currentDatabase) {
    const queries = storage.get(QUERIES_LS_KEY);

    if (queries == null) {
      storage.set(QUERIES_LS_KEY, JSON.stringify({ [currentDatabase]: {} }));
      return {};
    }

    const queriesObject = JSON.parse(queries);
    // If there is not object associated to the current database we return empty object
    if (!(currentDatabase in queriesObject)) {
      return {};
    }
    return queriesObject[currentDatabase];
  },
  addFavQuery(queryName, queryValue, currentDatabase) {
    const queries = this.getFavQueries(currentDatabase);

    queries[queryName] = queryValue;
    this.setFavQueries(queries, currentDatabase);
  },
  removeFavQuery(queryName, currentDatabase) {
    const queries = this.getFavQueries(currentDatabase);
    delete queries[queryName];
    this.setFavQueries(queries, currentDatabase);
  },
  setFavQueries(queriesParam, currentDatabase) {
    const queries = JSON.parse(storage.get(QUERIES_LS_KEY));
    Object.assign(queries, { [currentDatabase]: queriesParam });
    storage.set(QUERIES_LS_KEY, JSON.stringify(queries));
  },
};
