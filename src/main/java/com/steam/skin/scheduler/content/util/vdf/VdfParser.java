package com.steam.skin.scheduler.content.util.vdf;

import com.steam.skin.scheduler.content.entity.vdf.VdfNode;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public final class VdfParser {

    private final String input;
    private int position;

    private VdfParser(String input) {
        this.input = input;
    }

    public static VdfNode parse(byte[] data) {
        if (data == null) {
            throw new IllegalArgumentException("KV data is null");
        }

        String text = new String(
                data,
                StandardCharsets.UTF_8
        );

        if (!text.isEmpty() && text.charAt(0) == '\uFEFF') {
            text = text.substring(1);
        }

        VdfParser parser = new VdfParser(text);

        return parser.parseRoot();
    }

    private VdfNode parseRoot() {
        List<VdfNode> children = new ArrayList<>();

        skipWhitespaceAndComments();

        while (!eof()) {

            if (peek() == '}') {
                throw error("Unexpected '}'");
            }

            children.add(parseEntry());

            skipWhitespaceAndComments();
        }

        return VdfNode.object("__root__", children);
    }

    private VdfNode parseEntry() {
        skipWhitespaceAndComments();

        String key = readToken();

        if (key.isEmpty()) {
            throw error("Expected key");
        }

        skipWhitespaceAndComments();

        if (eof()) {
            throw error(
                    "Unexpected end of input after key: " + key
            );
        }

        if (peek() == '{') {

            position++;

            List<VdfNode> children = new ArrayList<>();

            skipWhitespaceAndComments();

            while (!eof() && peek() != '}') {
                children.add(parseEntry());
                skipWhitespaceAndComments();
            }

            if (eof()) {
                throw error(
                        "Missing '}' for key: " + key
                );
            }

            position++;

            return VdfNode.object(key, children);
        }
        String value = readToken();

        return VdfNode.value(key, value);
    }


    private String readToken() {

        skipWhitespaceAndComments();

        if (eof()) {
            throw error("Unexpected end of input");
        }

        if (peek() == '"') {
            return readQuotedString();
        }

        return readUnquotedToken();
    }


    private String readQuotedString() {

        if (peek() != '"') {
            throw error("Expected '\"'");
        }

        position++;

        StringBuilder result = new StringBuilder();

        while (!eof()) {

            char c = input.charAt(position++);
            if (c == '"') {
                return result.toString();
            }

            if (c == '\\') {

                if (eof()) {
                    throw error(
                            "Unexpected end of input after '\\'"
                    );
                }

                char escaped = input.charAt(position++);
                switch (escaped) {

                    case 'n' -> result.append('\n');
                    case 'r' -> result.append('\r');
                    case 't' -> result.append('\t');
                    case 'b' -> result.append('\b');
                    case 'f' -> result.append('\f');
                    case '\\' -> result.append('\\');
                    case '"' -> result.append('"');

                    default ->
                            result.append(escaped);
                }

                continue;
            }

            result.append(c);
        }

        throw error(
                "Unterminated quoted string"
        );
    }

    private String readUnquotedToken() {

        StringBuilder result = new StringBuilder();

        while (!eof()) {

            char c = peek();

            if (Character.isWhitespace(c)) {
                break;
            }

            if (c == '{' || c == '}') {
                break;
            }

            result.append(c);
            position++;
        }

        return result.toString();
    }

    private void skipWhitespaceAndComments() {

        while (!eof()) {

            if (Character.isWhitespace(peek())) {
                position++;
                continue;
            }

            if (peek() == '/'
                    && position + 1 < input.length()
                    && input.charAt(position + 1) == '/') {

                position += 2;

                while (!eof()
                        && input.charAt(position) != '\n'
                        && input.charAt(position) != '\r') {

                    position++;
                }

                continue;
            }

            break;
        }
    }

    private boolean eof() {
        return position >= input.length();
    }

    private char peek() {
        return input.charAt(position);
    }

    private IllegalStateException error(String message) {
        return new IllegalStateException(
                message +
                        " at position " +
                        position
        );
    }
}
