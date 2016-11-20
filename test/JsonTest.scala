/**
  * Created by bill on 11/18/2016.
  */
import org.json4s._
import org.json4s.native.JsonMethods._

implicit val formats = DefaultFormats
val json = parse("""{"foo":"bar"}""")

case class Baz(foo: String)