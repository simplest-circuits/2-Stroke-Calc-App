const { createRouteContext } = require("./routes/createRouteContext");
const { registerHealthRoutes } = require("./routes/health");
const { registerConfigRoutes } = require("./routes/config");
const { registerUserRoutes } = require("./routes/user");
const { registerAdminRoutes } = require("./routes/admin");

function registerHttpRoutes(app, ctx) {
  const routeCtx = createRouteContext(ctx);
  registerHealthRoutes(app, routeCtx);
  registerConfigRoutes(app, routeCtx);
  registerUserRoutes(app, routeCtx);
  registerAdminRoutes(app, routeCtx);
}

module.exports = { registerHttpRoutes };
