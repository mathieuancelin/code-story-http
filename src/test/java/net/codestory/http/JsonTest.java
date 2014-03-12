/**
 * Copyright (C) 2013 all@code-story.net
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License
 */
package net.codestory.http;

import static java.nio.charset.StandardCharsets.*;
import static org.reactivecouchbase.json.Syntax.*;
import static org.reactivecouchbase.json.JsResult.*;

import java.io.*;
import java.util.*;

import net.codestory.http.annotations.*;
import net.codestory.http.payload.*;
import net.codestory.http.testhelpers.*;

import org.reactivecouchbase.json.*;
import org.reactivecouchbase.common.Functionnal;
import com.google.common.base.Function;

import org.junit.*;

public class JsonTest extends AbstractWebServerTest {
  @Test
  public void json_serialization() {

    JsValue personJsValue = Json.obj(
      $("name", "John"),
      $("surname", "Doe"),
      $("age", 42),
      $("address", Json.obj(
        $("number", "221b"),
        $("street", "Baker Street"),
        $("city", "London")
      ))
    );

    String expectedPerson = Json.stringify(personJsValue);

    Person personObj = new Person("John", "Doe", 42, 
            new Address("221b", "Baker Street", "London"));

    server.configure(routes -> routes.
        get("/person.json", personJsValue).
        get("/person.obj", Json.toJson(personObj, Person.FORMAT)));

    get("/person.json").produces("application/json", expectedPerson);
    get("/person.obj").produces("application/json", expectedPerson);
  }  

  @Test
  public void json_post() {

    JsValue personJsValue = Json.obj(
      $("name", "John"),
      $("surname", "Doe"),
      $("age", 42),
      $("address", Json.obj(
        $("number", "221b"),
        $("street", "Baker Street"),
        $("city", "London")
      ))
    );

    JsValue badPersonJsValue = Json.obj(
      $("name", "John"),
      $("surname", "Doe"),
      $("age", 42),
      $("adresse", Json.obj(
        $("number", "221b"),
        $("street", "Baker Street"),
        $("city", "London")
      ))
    );

    String expectedPerson = Json.stringify(personJsValue);
    String badPerson = Json.stringify(badPersonJsValue);

    server.configure(routes -> routes.
        post("/persons/surname", context -> context.payload(JsValue.class).field("surname").as(String.class)).
        post("/persons/validate", context -> context.payload(JsValue.class).validate(Person.FORMAT).isSuccess()).
        post("/persons/extract", context -> context.payload(JsValue.class).read(Person.FORMAT).getOpt().map(p -> p.age).getOrElse(0)));

    post("/persons/surname", expectedPerson).produces("Doe");
    post("/persons/extract", expectedPerson).produces("42");
    post("/persons/validate", expectedPerson).produces("true");
    post("/persons/validate", badPerson).produces("false");
  }  

  public static class Address {
    public final String number;
    public final String street;
    public final String city;
    public Address(String number, String street, String city) {
      this.number = number;
      this.street = street;
      this.city = city;
    }
    public static final Format<Address> FORMAT =  new Format<Address>() {
      @Override
      public JsResult<Address> read(JsValue value) {
        return combine(
          value.field("number").read(String.class),
          value.field("street").read(String.class),
          value.field("city").read(String.class)
        ).map(input -> new Address(input._1, input._2, input._3));
      }
      @Override
      public JsValue write(Address value) {
        return Json.obj(
          $("number", value.number),
          $("street", value.street),
          $("city", value.city)
        );
      }
    };
  }

  public static class Person {
    public final String name;
    public final String surname;
    public final Integer age;
    public final Address address;
    public Person(String name, String surname, Integer age, Address address) {
      this.name = name;
      this.surname = surname;
      this.age = age;
      this.address = address;
    }
    public static final Format<Person> FORMAT = new Format<Person>() {
      @Override
      public JsResult<Person> read(JsValue value) {
        System.out.println(Json.stringify(value));
        return combine(
          value.field("name").read(String.class),
          value.field("surname").read(String.class),
          value.field("age").read(Integer.class),
          value.field("address").read(Address.FORMAT)
        ).map(input -> new Person(input._1, input._2, input._3, input._4));
      }
      @Override
      public JsValue write(Person value) {
        return Json.obj(
          $("name", value.name),
          $("surname", value.surname),
          $("age", value.age),
          $("address", Address.FORMAT.write(value.address))
        );
      }
    };
  }
}
