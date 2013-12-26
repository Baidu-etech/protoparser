// Copyright 2013 Square, Inc.
package com.squareup.protoparser;

import com.squareup.protoparser.EnumType.Value;
import com.squareup.protoparser.MessageType.Label;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.fest.assertions.api.Fail;
import org.junit.Test;

import static org.fest.assertions.api.Assertions.assertThat;

public final class ProtoSchemaParserTest {
  private static final List<Type> NO_TYPES = Collections.emptyList();
  private static final List<Service> NO_SERVICES = Collections.emptyList();
  private static final List<String> NO_STRINGS = Collections.emptyList();
  private static final List<ExtendDeclaration> NO_EXTEND_DECLARATIONS = Collections.emptyList();
  private static final List<Extensions> NO_EXTENSIONS = Collections.emptyList();
  private static final List<Option> NO_OPTIONS = Collections.emptyList();

  @Test public void field() throws Exception {
    MessageType.Field field = new MessageType.Field(Label.OPTIONAL, "CType", "ctype", 1, "",
        list(new Option("default", "STRING"), new Option("deprecated", "true")));
    assertThat(field.isDeprecated()).isTrue();
    assertThat(field.getDefault()).isEqualTo("STRING");
    assertThat(field.getExtensions()).isEqualTo(map("default", "STRING", "deprecated", "true"));
  }

  @Test public void singleLineComment() {
    String proto = ""
        + "// Test all the things!\n"
        + "message Test {}";
    ProtoFile parsed = ProtoSchemaParser.parse("test.proto", proto);
    MessageType type = (MessageType) parsed.getTypes().get(0);
    assertThat(type.getDocumentation()).isEqualTo("Test all the things!");
  }

  @Test public void multipleSingleLineComments() {
    String proto = ""
        + "// Test all\n"
        + "// the things!\n"
        + "message Test {}";
    String expected = ""
        + "Test all\n"
        + "the things!";
    ProtoFile parsed = ProtoSchemaParser.parse("test.proto", proto);
    MessageType type = (MessageType) parsed.getTypes().get(0);
    assertThat(type.getDocumentation()).isEqualTo(expected);
  }

  @Test public void singleLineJavadocComment() {
    String proto = ""
        + "/** Test */\n"
        + "message Test {}";
    ProtoFile parsed = ProtoSchemaParser.parse("test.proto", proto);
    MessageType type = (MessageType) parsed.getTypes().get(0);
    assertThat(type.getDocumentation()).isEqualTo("Test");
  }

  @Test public void multilineJavadocComment() {
    String proto = ""
        + "/**\n"
        + " * Test\n"
        + " *\n"
        + " * Foo\n"
        + " */\n"
        + "message Test {}";
    String expected = ""
        + "Test\n"
        + "\n"
        + "Foo";
    ProtoFile parsed = ProtoSchemaParser.parse("test.proto", proto);
    MessageType type = (MessageType) parsed.getTypes().get(0);
    assertThat(type.getDocumentation()).isEqualTo(expected);
  }

  @Test public void multipleSingleLineCommentsWithLeadingWhitespace() {
    String proto = ""
        + "// Test\n"
        + "//   All\n"
        + "//     The\n"
        + "//       Things!\n"
        + "message Test {}";
    String expected = ""
        + "Test\n"
        + "  All\n"
        + "    The\n"
        + "      Things!";
    ProtoFile parsed = ProtoSchemaParser.parse("test.proto", proto);
    MessageType type = (MessageType) parsed.getTypes().get(0);
    assertThat(type.getDocumentation()).isEqualTo(expected);
  }

  @Test public void multilineJavadocCommentWithLeadingWhitespace() {
    String proto = ""
        + "/**\n"
        + " * Test\n"
        + " *   All\n"
        + " *     The\n"
        + " *       Things!\n"
        + " */\n"
        + "message Test {}";
    String expected = ""
        + "Test\n"
        + "  All\n"
        + "    The\n"
        + "      Things!";
    ProtoFile parsed = ProtoSchemaParser.parse("test.proto", proto);
    MessageType type = (MessageType) parsed.getTypes().get(0);
    assertThat(type.getDocumentation()).isEqualTo(expected);
  }

  @Test public void multilineJavadocCommentWithoutLeadingAsterisks() {
    // We do not honor leading whitespace when the comment lacks leading asterisks.
    String proto = ""
        + "/**\n"
        + " Test\n"
        + "   All\n"
        + "     The\n"
        + "       Things!\n"
        + " */\n"
        + "message Test {}";
    String expected = ""
        + "Test\n"
        + "All\n"
        + "The\n"
        + "Things!";
    ProtoFile parsed = ProtoSchemaParser.parse("test.proto", proto);
    MessageType type = (MessageType) parsed.getTypes().get(0);
    assertThat(type.getDocumentation()).isEqualTo(expected);
  }

  @Test public void messageFieldTrailingComment() {
    // Trailing message field comment.
    String proto = ""
        + "message Test {\n"
        + "  optional string name = 1; // Test all the things!\n"
        + "}";
    ProtoFile parsed = ProtoSchemaParser.parse("test.proto", proto);
    MessageType.Field field = ((MessageType) parsed.getTypes().get(0)).getFields().get(0);
    assertThat(field.getDocumentation()).isEqualTo("Test all the things!");
  }

  @Test public void messageFieldLeadingAndTrailingCommentAreCombined() {
    String proto = ""
        + "message Test {\n"
        + "  // Test all...\n"
        + "  optional string name = 1; // ...the things!\n"
        + "}";
    ProtoFile parsed = ProtoSchemaParser.parse("test.proto", proto);
    MessageType.Field field = ((MessageType) parsed.getTypes().get(0)).getFields().get(0);
    assertThat(field.getDocumentation()).isEqualTo("Test all...\n...the things!");
  }

  @Test public void trailingCommentNotAssignedToFollowingField() {
    String proto = ""
        + "message Test {\n"
        + "  optional string first_name = 1; // Testing!\n"
        + "  optional string last_name = 2;\n"
        + "}";
    ProtoFile parsed = ProtoSchemaParser.parse("test.proto", proto);
    MessageType message = (MessageType) parsed.getTypes().get(0);
    MessageType.Field field1 = message.getFields().get(0);
    assertThat(field1.getDocumentation()).isEqualTo("Testing!");
    MessageType.Field field2 = message.getFields().get(1);
    assertThat(field2.getDocumentation()).isEqualTo("");
  }

  @Test public void enumValueTrailingComment() {
    String proto = ""
        + "enum Test {\n"
        + "  FOO = 1; // Test all the things!\n"
        + "}";
    ProtoFile parsed = ProtoSchemaParser.parse("test.proto", proto);
    Value value = ((EnumType) parsed.getTypes().get(0)).getValues().get(0);
    assertThat(value.getDocumentation()).isEqualTo("Test all the things!");
  }

  @Test public void enumValueLeadingAndTrailingCommentsAreCombined() {
    String proto = ""
        + "enum Test {\n"
        + "  // Test all...\n"
        + "  FOO = 1; // ...the things!\n"
        + "}";
    ProtoFile parsed = ProtoSchemaParser.parse("test.proto", proto);
    Value value = ((EnumType) parsed.getTypes().get(0)).getValues().get(0);
    assertThat(value.getDocumentation()).isEqualTo("Test all...\n...the things!");
  }

  @Test public void parseMessageAndFields() throws Exception {
    String proto = ""
        + "message SearchRequest {\n"
        + "  required string query = 1;\n"
        + "  optional int32 page_number = 2;\n"
        + "  optional int32 result_per_page = 3;\n"
        + "}";
    Type expected = new MessageType("SearchRequest", "SearchRequest", "",
        Arrays.asList(new MessageType.Field(Label.REQUIRED, "string", "query", 1, "", NO_OPTIONS),
            new MessageType.Field(Label.OPTIONAL, "int32", "page_number", 2, "", NO_OPTIONS),
            new MessageType.Field(Label.OPTIONAL, "int32", "result_per_page", 3, "", NO_OPTIONS)),
        NO_TYPES, NO_EXTENSIONS, NO_OPTIONS);
    ProtoFile protoFile =
        new ProtoFile("search.proto", null, NO_STRINGS, NO_STRINGS, Arrays.asList(expected),
            NO_SERVICES, map(), NO_EXTEND_DECLARATIONS);
    assertThat(ProtoSchemaParser.parse("search.proto", new StringReader(proto))).isEqualTo(protoFile);
  }

  @Test public void parseEnum() throws Exception {
    String proto = ""
        + "/**\n"
        + " * What's on my waffles.\n"
        + " * Also works on pancakes.\n"
        + " */\n"
        + "enum Topping {\n"
        + "  FRUIT = 1;\n"
        + "  /** Yummy, yummy cream. */\n"
        + "  CREAM = 2;\n"
        + "\n"
        + "  // Quebec Maple syrup\n"
        + "  SYRUP = 3;\n"
        + "}\n";
    Type expected = new EnumType("Topping", "Topping",
       "What's on my waffles.\nAlso works on pancakes.",
        Arrays.asList(new Value("FRUIT", 1, "", NO_OPTIONS), new Value("CREAM", 2, "Yummy, yummy cream.",
            NO_OPTIONS), new Value("SYRUP", 3, "Quebec Maple syrup", NO_OPTIONS)));
    ProtoFile protoFile =
        new ProtoFile("waffles.proto", null, NO_STRINGS, NO_STRINGS, Arrays.asList(expected),
            NO_SERVICES, map(), NO_EXTEND_DECLARATIONS);
    ProtoFile actual = ProtoSchemaParser.parse("waffles.proto", proto);
    assertThat(actual).isEqualTo(protoFile);
  }

  @Test public void packageDeclaration() throws Exception {
    String proto = ""
        + "package google.protobuf;\n"
        + "option java_package = \"com.google.protobuf\";\n"
        + "\n"
        + "// The protocol compiler can output a FileDescriptorSet containing the .proto\n"
        + "// files it parses.\n"
        + "message FileDescriptorSet {\n"
        + "}\n";
    Type message = new MessageType("FileDescriptorSet", "google.protobuf.FileDescriptorSet", ""
        + "The protocol compiler can output a FileDescriptorSet containing the .proto\n"
        + "files it parses.", Arrays.<MessageType.Field>asList(), NO_TYPES, NO_EXTENSIONS,
        NO_OPTIONS);
    ProtoFile expected =
        new ProtoFile("descriptor.proto", "google.protobuf", NO_STRINGS, NO_STRINGS,
            Arrays.asList(message), NO_SERVICES, map("java_package", "com.google.protobuf"),
            NO_EXTEND_DECLARATIONS);
    assertThat(ProtoSchemaParser.parse("descriptor.proto", proto))
        .isEqualTo(expected);
  }

  @Test public void nestingInMessage() throws Exception {
    String proto = ""
        + "message FieldOptions {\n"
        + "  optional CType ctype = 1 [default = STRING, deprecated=true];\n"
        + "  enum CType {\n"
        + "    STRING = 0[(opt_a) = 1, (opt_b) = 2];\n"
        + "  };\n"
        + "  // Clients can define custom options in extensions of this message. See above.\n"
        + "  extensions 500;\n"
        + "  extensions 1000 to max;\n"
        + "}\n";
    Type enumType = new EnumType("CType", "FieldOptions.CType", "",
        Arrays.asList(new Value("STRING", 0, "",
            Arrays.asList(new Option("opt_a", "1"), new Option("opt_b", "2")))));
    MessageType.Field field = new MessageType.Field(Label.OPTIONAL, "CType", "ctype", 1, "",
        list(new Option("default", "STRING"), new Option("deprecated", "true")));
    assertThat(field.getExtensions()).isEqualTo(map("default", "STRING", "deprecated", "true"));

    Type messageType = new MessageType("FieldOptions", "FieldOptions", "", Arrays.asList(field),
            Arrays.asList(enumType), NO_EXTENSIONS, NO_OPTIONS);
    ProtoFile expected =
        new ProtoFile("descriptor.proto", null, NO_STRINGS, NO_STRINGS, Arrays.asList(messageType),
            NO_SERVICES, map(), NO_EXTEND_DECLARATIONS);
    ProtoFile actual = ProtoSchemaParser.parse("descriptor.proto", proto);
    assertThat(actual).isEqualTo(expected);
  }

  @Test public void imports() throws Exception {
    String proto = "import \"src/test/resources/unittest_import.proto\";\n";
    ProtoFile expected = new ProtoFile("descriptor.proto", null,
        Arrays.asList("src/test/resources/unittest_import.proto"), NO_STRINGS, NO_TYPES,
        NO_SERVICES, map(), NO_EXTEND_DECLARATIONS);
    assertThat(ProtoSchemaParser.parse("descriptor.proto", proto))
        .isEqualTo(expected);
  }

  @Test public void publicImports() throws Exception {
    String proto = "import public \"src/test/resources/unittest_import.proto\";\n";
    ProtoFile expected = new ProtoFile("descriptor.proto", null,
        NO_STRINGS, Arrays.asList("src/test/resources/unittest_import.proto"), NO_TYPES,
        NO_SERVICES, map(), NO_EXTEND_DECLARATIONS);
    assertThat(ProtoSchemaParser.parse("descriptor.proto", proto))
        .isEqualTo(expected);
  }

  @Test public void extend() throws Exception {
    String proto = ""
        + "// Extends Foo\n"
        + "extend Foo {\n"
        + "  optional int32 bar = 126;\n"
        + "}";
    List<ExtendDeclaration> extendDeclarations = new ArrayList<ExtendDeclaration>();
    extendDeclarations.add(new ExtendDeclaration("Foo", "Foo", "Extends Foo",
        Arrays.asList(new MessageType.Field(Label.OPTIONAL, "int32", "bar", 126, "", NO_OPTIONS))));
    ProtoFile expected =
        new ProtoFile("descriptor.proto", null, NO_STRINGS, NO_STRINGS, NO_TYPES, NO_SERVICES,
            map(), extendDeclarations);
    assertThat(ProtoSchemaParser.parse("descriptor.proto", proto))
        .isEqualTo(expected);
  }

  @Test public void defaultFieldWithParen() throws Exception {
    String proto = ""
        + "message Foo {\n"
        + "  optional string claim_token = 2 [(squareup.redacted) = true];\n"
        + "}";
    MessageType.Field field = new MessageType.Field(Label.OPTIONAL, "string", "claim_token", 2, "",
        list(new Option("squareup.redacted", "true")));
    assertThat(field.getExtensions()).isEqualTo(map("squareup.redacted", "true"));

    Type messageType = new MessageType("Foo", "Foo", "", Arrays.asList(field), NO_TYPES,
        NO_EXTENSIONS, NO_OPTIONS);
    ProtoFile expected = new ProtoFile("descriptor.proto", null, NO_STRINGS, NO_STRINGS,
        Arrays.<Type>asList(messageType), NO_SERVICES, map(), NO_EXTEND_DECLARATIONS);
    assertThat(ProtoSchemaParser.parse("descriptor.proto", proto))
        .isEqualTo(expected);
  }

  // Parse \a, \b, \f, \n, \r, \t, \v, \[0-7]{1-3}, and \[xX]{0-9a-fA-F]{1,2}
  @Test public void defaultFieldWithStringEscapes() throws Exception {
    String proto = ""
        + "message Foo {\n"
        + "  optional string name = 1 "
        + "[default = \"\\a\\b\\f\\n\\r\\t\\v\1f\01\001\11\011\111\\xe\\Xe\\xE\\xE\\x41\\X41\"];\n"
        + "}";
    MessageType.Field field = new MessageType.Field(Label.OPTIONAL, "string", "name", 1, "", list(
        new Option("default",
            "\u0007\b\f\n\r\t\u000b\u0001f\u0001\u0001\u0009\u0009I\u000e\u000e\u000e\u000eAA")));
    assertThat(field.getExtensions()).isEqualTo(map("default",
        "\u0007\b\f\n\r\t\u000b\u0001f\u0001\u0001\u0009\u0009I\u000e\u000e\u000e\u000eAA"));

    Type messageType = new MessageType("Foo", "Foo", "", Arrays.asList(field),
            NO_TYPES, NO_EXTENSIONS, NO_OPTIONS);
    ProtoFile expected =
        new ProtoFile("foo.proto", null, NO_STRINGS, NO_STRINGS, Arrays.<Type>asList(messageType),
            NO_SERVICES, map(), NO_EXTEND_DECLARATIONS);
    assertThat(ProtoSchemaParser.parse("foo.proto", proto))
        .isEqualTo(expected);
  }

  @Test public void invalidHexStringEscape() throws Exception {
    String proto = ""
        + "message Foo {\n"
        + "  optional string name = 1 "
        + "[default = \"\\xW\"];\n"
        + "}";
    try {
      ProtoSchemaParser.parse("foo.proto", proto);
      Fail.fail("Expected parse error");
    } catch (IllegalStateException e) {
      assertThat(e.getMessage().contains("expected a digit after \\x or \\X"));
    }
  }

  @Test public void service() throws Exception {
    String proto = ""
        + "service SearchService {\n"
        + "  rpc Search (SearchRequest) returns (SearchResponse);"
        + "  rpc Purchase (PurchaseRequest) returns (PurchaseResponse) {\n"
        + "    option (squareup.sake.timeout) = 15; \n"
        + "    option (squareup.a.b) = { value: [FOO, BAR] };\n"
        + "  }\n"
        + "}";
    Service expected = new Service("SearchService", "SearchService", "", Arrays.asList(
        new Service.Method("Search", "", "SearchRequest", "SearchResponse", map()),
        new Service.Method("Purchase", "", "PurchaseRequest", "PurchaseResponse",
            map("squareup.sake.timeout", "15", "squareup.a.b", map("value", list("FOO", "BAR"))))));
    ProtoFile protoFile = new ProtoFile("descriptor.proto", null, NO_STRINGS, NO_STRINGS, NO_TYPES,
        Arrays.asList(expected), map(), NO_EXTEND_DECLARATIONS);
    assertThat(ProtoSchemaParser.parse("descriptor.proto", proto))
        .isEqualTo(protoFile);
  }

  @Test public void hexTag() throws Exception {
    String proto = ""
        + "message HexTag {\n"
        + "  required string hex = 0x10;\n"
        + "}";
    Type expected = new MessageType("HexTag", "HexTag", "",
        Arrays.asList(new MessageType.Field(Label.REQUIRED, "string", "hex", 16, "", NO_OPTIONS)),
        NO_TYPES, NO_EXTENSIONS, NO_OPTIONS);
    ProtoFile protoFile =
        new ProtoFile("hex.proto", null, NO_STRINGS, NO_STRINGS, Arrays.asList(expected),
            NO_SERVICES, map(), NO_EXTEND_DECLARATIONS);
    assertThat(ProtoSchemaParser.parse("hex.proto", proto)).isEqualTo(protoFile);
  }

  @Test public void structuredOption() throws Exception {
    String proto = ""
        + "message ExoticOptions {\n"
        + "  option (squareup.one) = {name: \"Name\", class_name:\"ClassName\"};\n"
        + "  option (squareup.two.a) = {[squareup.options.type]: EXOTIC};\n"
        + "  option (squareup.two.b) = {names: [\"Foo\", \"Bar\"]};\n"
        + "  option (squareup.three) = {x: {y: 1 y: 2}};\n" // NOTE: Omitted optional comma
        + "  option (squareup.four) = {x: {y: {z: 1}, y: {z: 2}}};\n"
        + "}";

    List<Option> options = new ArrayList<Option>();
    Map<String, String> option_one_map = new LinkedHashMap<String, String>();
    option_one_map.put("name", "Name");
    option_one_map.put("class_name", "ClassName");
    options.add(new Option("squareup.one", option_one_map));
    Map<String, String> option_two_a_map = new LinkedHashMap<String, String>();
    option_two_a_map.put("[squareup.options.type]", "EXOTIC");
    options.add(new Option("squareup.two.a", option_two_a_map));
    Map<String, List<String>> option_two_b_map = new LinkedHashMap<String, List<String>>();
    option_two_b_map.put("names", Arrays.asList("Foo", "Bar"));
    options.add(new Option("squareup.two.b", option_two_b_map));
    Map<String, Map<String, ?>> option_three_map = new LinkedHashMap<String, Map<String, ?>>();
    Map<String, List<String>> option_three_nested_map = new LinkedHashMap<String, List<String>>();
    option_three_nested_map.put("y", Arrays.asList("1", "2"));
    option_three_map.put("x", option_three_nested_map);
    options.add(new Option("squareup.three", option_three_map));

    Map<String, Map<String, ?>> option_four_map = new LinkedHashMap<String, Map<String, ?>>();
    Map<String, Object> option_four_map_1 = new LinkedHashMap<String, Object>();
    Map<String, String> option_four_map_2_a = new LinkedHashMap<String, String>();
    option_four_map_2_a.put("z", "1");
    Map<String, String> option_four_map_2_b = new LinkedHashMap<String, String>();
    option_four_map_2_b.put("z", "2");
    option_four_map_1.put("y", Arrays.asList(option_four_map_2_a, option_four_map_2_b));
    option_four_map.put("x", option_four_map_1);
    options.add(new Option("squareup.four", option_four_map));

    Type expected =
        new MessageType("ExoticOptions", "ExoticOptions", "", Arrays.<MessageType.Field>asList(),
            NO_TYPES, NO_EXTENSIONS, options);
    ProtoFile protoFile =
        new ProtoFile("exotic.proto", null, NO_STRINGS, NO_STRINGS, Arrays.asList(expected),
            NO_SERVICES, map(), NO_EXTEND_DECLARATIONS);
    assertThat(ProtoSchemaParser.parse("exotic.proto", proto)).isEqualTo(protoFile);
  }

  @Test public void optionsWithNestedMapsAndTrailingCommas() throws Exception {
    String proto = ""
        + "message StructuredOption {\n"
        + "    optional field.type has_options = 3 [\n"
        + "            (option_map) = {\n"
        + "                nested_map: {key:\"value\" key2:[\"value2a\",\"value2b\"]},\n"
        + "            }\n"
        + "            (option_string) = [\"string1\",\"string2\"]\n"
        + "    ];\n"
        + "}";
    MessageType.Field field = new MessageType.Field(Label.OPTIONAL, "field.type", "has_options", 3,
        "", list(new Option("option_map", map("nested_map", map("key", "value", "key2",
        list("value2a", "value2b")))), new Option("option_string", list("string1","string2"))));
    assertThat(field.getExtensions())
        .isEqualTo(map("option_map", map("nested_map", map("key", "value", "key2",
            list("value2a", "value2b"))), "option_string", list("string1", "string2")));

    Type expected = new MessageType("StructuredOption", "StructuredOption", "",
        Arrays.<MessageType.Field>asList(field), NO_TYPES, NO_EXTENSIONS, NO_OPTIONS);
    ProtoFile protoFile =
        new ProtoFile("nestedmaps.proto", null, NO_STRINGS, NO_STRINGS, Arrays.asList(expected),
            NO_SERVICES, map(), NO_EXTEND_DECLARATIONS);
    assertThat(ProtoSchemaParser.parse("nestedmaps.proto", proto))
        .isEqualTo(protoFile);
  }

  @Test public void extensionWithNestedMessage() throws Exception {
    String proto = ""
        + "message Foo {\n"
        + "  optional int32 bar = 1 [\n"
        + "      (validation.range).min = 1,\n"
        + "      (validation.range).max = 100,\n"
        + "      default = 20\n"
        + "  ];\n"
        + "}";
    MessageType.Field field = new MessageType.Field(Label.OPTIONAL, "int32", "bar", 1, "",
        list(new Option("validation.range", new Option("min", "1")),
            new Option("validation.range", new Option("max", "100")), new Option("default", "20")));
    assertThat(field.getExtensions())
        .isEqualTo(map("validation.range", map("min", "1", "max", "100"), "default", "20"));

    Type expected = new MessageType("Foo", "Foo", "", Arrays.asList(field), NO_TYPES, NO_EXTENSIONS,
        NO_OPTIONS);
    ProtoFile protoFile =
        new ProtoFile("foo.proto", null, NO_STRINGS, NO_STRINGS, Arrays.asList(expected),
            NO_SERVICES, map(), NO_EXTEND_DECLARATIONS);
    assertThat(ProtoSchemaParser.parse("foo.proto", proto)).isEqualTo(protoFile);
  }

  @Test public void parseBadTagNumber() throws Exception {
    String proto = ""
        + "message BadTagNumber {\n"
        + "  required int32 a = 0;\n"
        + "}";
    try {
      ProtoSchemaParser.parse("badtag.proto", proto);
      Fail.fail("Expected parse error");
    } catch (Exception e) {
      assertThat(e.getMessage().contains("expected tag > 0"));
    }
  }

  private Map<String, Object> map(Object... keysAndValues) {
    Map<String, Object> result = new LinkedHashMap<String, Object>();
    for (int i = 0; i < keysAndValues.length; i += 2) {
      result.put((String) keysAndValues[i], keysAndValues[i + 1]);
    }
    return result;
  }

  private <T> List<T> list(T... values) {
    return Arrays.asList(values);
  }
}
