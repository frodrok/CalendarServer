import javax.inject._

import akka.stream.Materializer
import play.api._
import play.api.http.HttpFilters
import play.api.mvc._
import filters.ExampleFilter

import scala.concurrent.{ExecutionContext, Future}

/**
 * This class configures filters that run on every request. This
 * class is queried by Play to get a list of filters.
 *
 * Play will automatically use filters from any class called
 * `Filters` that is placed the root package. You can load filters
 * from a different class by adding a `play.http.filters` setting to
 * the `application.conf` configuration file.
 *
 * @param env Basic environment settings for the current application.
 * @param exampleFilter A demonstration filter that adds a header to
 * each response.
 */
@Singleton
class Filters @Inject() (env: Environment,
                          exampleFilter: ExampleFilter, accessControll: AccessControll) extends HttpFilters {

  override val filters = {
    // Use the example filter if we're running development mode. If
    // we're running in production or test mode then don't use any
    // filters at all.
    //if (env.mode == Mode.Dev) Seq(exampleFilter) else Seq.empty
    Seq.apply(accessControll)

  }

}

@Singleton
class AccessControll @Inject()(
                               implicit override val mat: Materializer,
                               exec: ExecutionContext) extends Filter {

  override def apply(nextFilter: RequestHeader => Future[Result])
                    (requestHeader: RequestHeader): Future[Result] = {
    // Run the next filter in the chain. This will call other filters
    // and eventually call the action. Take the result and modify it
    // by adding a new header.
    nextFilter(requestHeader).map { result =>
      result.withHeaders(
        "Access-Control-Allow-Origin" -> "*",
        "Access-Control-Allow-Methods" -> "GET, POST, OPTIONS, DELETE, PUT",
        "Access-Control-Allow-Headers" -> "Accept, Origin, Content-type, X-Json, X-Prototype-Version, X-Requested-With",
        "Access-Control-Allow-Credentials" -> "true",
        "Access-Control-Max-Age" -> (60 * 60 * 24).toString
      )
    }
  }

}
