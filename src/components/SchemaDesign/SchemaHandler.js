let tx;

function SchemaHandler(graknTx) {
  tx = graknTx;
}

function toGraknDatatype(valueTypeParam) {
  switch (valueTypeParam) {
    case 'string': return ValueType.STRING;
    case 'datetime': return ValueType.DATETIME;
    case 'boolean': return ValueType.BOOLEAN;
    case 'long': return ValueType.LONG;
    case 'double': return ValueType.DOUBLE;
    default: throw new Error(`Datatype not recognised. Received [${valueTypeParam}]`);
  }
}

SchemaHandler.prototype.defineEntityType = async function define(entityLabel, superType) {
  const type = await tx.concepts().putEntityType(entityLabel);
  const directSuper = await tx.concepts().getEntityType(superType);
  await type.asRemote(tx).setSupertype(directSuper);
};

SchemaHandler.prototype.defineRelationType = async function define(relationLabel, superType) {
  const type = await tx.concepts().putRelationType(relationLabel);
  const directSuper = await tx.concepts().getRelationType(superType);
  await type.asRemote(tx).setSupertype(directSuper);
  return type;
};

SchemaHandler.prototype.defineAttributeType = async function define(attributeLabel, superType, valueType) {
  const type = await tx.concepts().putAttributeType(attributeLabel, toGraknDatatype(valueType));
  const directSuper = await tx.concepts().getAttributeType(superType);
  await type.asRemote(tx).setSupertype(directSuper);
};

SchemaHandler.prototype.defineRule = async function define(ruleLabel, when, then) {
  return tx.logic().putRule(ruleLabel, when, then);
};

SchemaHandler.prototype.deleteType = async function deleteType(schemaLabel) {
  const type = await tx.concepts().getThingType(schemaLabel);
  await type.asRemote(tx).delete();
  return type.getLabel();
};

SchemaHandler.prototype.addAttribute = async function addAttribute(schemaLabel, attributeLabel) {
  const type = await tx.concepts().getThingType(schemaLabel);
  const attribute = await tx.concepts().getAttributeType(attributeLabel);
  return type.asRemote(tx).setOwns(attribute);
};

SchemaHandler.prototype.deleteAttribute = async function deleteAttribute(schemaLabel, attributeLabel) {
  const type = await tx.concepts().getThingType(schemaLabel);
  const attribute = await tx.concepts().getAttributeType(attributeLabel);
  return type.asRemote(tx).unsetOwns(attribute);
};

SchemaHandler.prototype.addPlaysRole = async function addPlaysRole(schemaLabel, relationLabel, roleLabel) {
  const type = await tx.concepts().getThingType(schemaLabel);
  const relation = await tx.concepts().getRelationType(relationLabel);
  const role = await relation.asRemote(tx).getRelates(roleLabel);
  return type.asRemote(tx).setPlays(role);
};

SchemaHandler.prototype.deletePlaysRole = async function deletePlaysRole(schemaLabel, relationLabel, roleLabel) {
  const type = await tx.concepts().getThingType(schemaLabel);
  const relation = await tx.concepts().getRelationType(relationLabel);
  const role = await relation.asRemote(tx).getRelates(roleLabel);
  return type.asRemote(tx).unsetPlays(role);
};

SchemaHandler.prototype.addRelatesRole = async function addRelatesRole(schemaLabel, roleLabel) {
  const relationType = await tx.concepts().getRelationType(schemaLabel);
  return relationType.asRemote(tx).setRelates(roleLabel);
};

SchemaHandler.prototype.deleteRelatesRole = async function deleteRelatesRole(schemaLabel, roleLabel) {
  const relationType = await tx.concepts().getRelationType(schemaLabel);
  return relationType.asRemote(tx).unsetRelates(roleLabel);
};

export default SchemaHandler;
