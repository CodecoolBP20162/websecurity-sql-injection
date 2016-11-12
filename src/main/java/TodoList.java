import model.*;
import spark.*;
import spark.template.velocity.*;
import java.util.*;
import static spark.Spark.*;

/**
 * This class uses the ICRoute interface to create void routes.
 * The response for an ICRoute is rendered in an after-filter.
 */
public class TodoList {

    private static TodoDaoImplWithList todoDao = new TodoDaoImplWithList();

    public static void main(String[] args) {

        exception(Exception.class, (e, req, res) -> e.printStackTrace()); // print all exceptions
        staticFiles.location("/public");
        port(9999);

        get("/",                        (req, res)      -> renderTodos(req));
        get("/todos/:id/edit",          (req, res)      -> renderEditTodo(req));

        post("/todos",                  (ICRoute) (req) -> todoDao.add(Todo.create(req.queryParams("todo-title"))));
        delete("/todos/completed",      (ICRoute) (req) -> todoDao.removeCompleted());
        delete("/todos/:id",            (ICRoute) (req) -> todoDao.remove(req.params("id")));
        put("/todos/toggle_status",     (ICRoute) (req) -> todoDao.toggleAll(req.queryParams("toggle-all") != null));
        put("/todos/:id",               (ICRoute) (req) -> todoDao.update(req.params("id"), req.queryParams("todo-title")));
        put("/todos/:id/toggle_status", (ICRoute) (req) -> todoDao.toggleStatus(req.params("id")));

        after((req, res) -> {
            if (res.body() == null) { // if we didn't try to return a rendered response
                res.body(renderTodos(req));
            }
        });

    }

    private static String renderEditTodo(Request req) {
        return renderTemplate("velocity/editTodo.vm", new HashMap(){{ put("todo", todoDao.find(req.params("id"))); }});
    }

    private static String renderTodos(Request req) {
        String statusStr = req.queryParams("status");
        Map<String, Object> model = new HashMap<>();
        model.put("todos", todoDao.ofStatus(statusStr));
        model.put("filter", Optional.ofNullable(statusStr).orElse(""));
        model.put("activeCount", todoDao.ofStatus(Status.ACTIVE).size());
        model.put("anyCompleteTodos", todoDao.ofStatus(Status.COMPLETE).size() > 0);
        model.put("allComplete", todoDao.all().size() == todoDao.ofStatus(Status.COMPLETE).size());
        model.put("status", Optional.ofNullable(statusStr).orElse(""));
        if ("true".equals(req.queryParams("ic-request"))) {
            return renderTemplate("velocity/todoList.vm", model);
        }
        return renderTemplate("velocity/index.vm", model);
    }

    private static String renderTemplate(String template, Map model) {
        return new VelocityTemplateEngine().render(new ModelAndView(model, template));
    }

    @FunctionalInterface
    private interface ICRoute extends Route {
        default Object handle(Request request, Response response) throws Exception {
            handle(request);
            return "";
        }
        void handle(Request request) throws Exception;
    }

}
