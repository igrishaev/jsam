package me.ivan;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.Stack;

public class Parser {

    public static final int bufSize = 32;
    private int r;
    private int off;
    private int len;
    private boolean eof;
    private final Reader reader;
    private final char[] buf;
    private StringBuilder stringBuilder;
    private Stack<Object> objects;
    private StringBuilder uXXXX;
    private Stack<State> states;

    public Parser(final Reader reader) {
        this.reader = reader;
        this.eof = false;
        this.r = 0;
        this.off = 0;
        this.len = bufSize;
        this.buf = new char[bufSize];
        this.stringBuilder = new StringBuilder();
        this.uXXXX = new StringBuilder(4);
        this.objects = new Stack<>();
        this.states = new Stack<>();

//        this.states.push(State.READING_ANY_DONE);

//        this.states.push(State.READ_ANY);
//        this.states.push(State.WS);

        plan(State.WS, State.ANY, State.DONE);
    }

    private void plan(final State... args) {
        for (int i = args.length; i > 0; i--) {
            states.push(args[i-1]);
        }
    }

    private static enum State {
        ANY,
        DONE,
        WS,
        COLON, QUOTE,

        ARR, ARR_NEXT,

        STR,
        STR_ESC,
        READING_STRING_ESCAPED_U,
        READING_STRING_ESCAPED_U_X,
        READING_STRING_ESCAPED_U_XX,
        READING_STRING_ESCAPED_U_XXX,
        N, NU, NUL,
        T, TR, TRU,
        F, FA, FAL, FALS,
        OBJ, OBJ_NEXT,

    }

    private void fillBuffer() throws IOException {
        off = 0;
        len = bufSize;
        while (!eof) {
            r = reader.read(buf, off, len);
            if (r == -1) {
                eof = true;
            } else if (r == 0) {
                break;
            } else {
                off += r;
                len -= r;
            }
        }
    }

    private void collapseList() {
        final Object last = objects.pop();
        final List list = (List) objects.peek();
        list.add(last);
    }

    private void collapseMap() {
        final Object v = objects.pop();
        final Object k = objects.pop();
        final Map map = (Map) objects.peek();
        map.put(k, v);
    }

    private static boolean isHex(final char c) {
        return switch (c) {
            case '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
                    'a', 'b', 'c', 'd', 'e', 'f',
                    'A', 'B', 'C', 'D', 'E', 'F' ->
                    true;
            default -> false;
        };
    }

    private void parseBuf() {
        char c;
        for (int i = 0; i < off; i++) {
            c = buf[i];
            switch (states.peek()) {

                case WS -> {
                    switch (c) {
                        case ' ', '\n', '\r', '\t' -> {}
                        default -> {
                            i--;
                            states.pop();
                        }
                    }
                }

                case ANY -> {
                    states.pop();
                    switch (c) {
                        case '"' -> states.push(State.STR);
                        case 'n' -> states.push(State.N);
                        case '{' -> {
                            objects.push(new HashMap<>());
                            plan(State.WS, State.OBJ);
                        }
                        case '[' -> {
                            objects.push(new ArrayList<>());
                            plan(State.WS, State.ARR);
                        }
                        default -> throw new RuntimeException(objects.toString());
                    }
                }
                case STR -> {
                    switch (c) {
                        case '\\' -> states.push(State.STR_ESC);
                        case '"' -> {
                            final String string = stringBuilder.toString();
                            stringBuilder.setLength(0);
                            objects.push(string);
                            states.pop();
                        }
                        default -> stringBuilder.append(c);
                    }
                }
                case STR_ESC -> {
                    switch (c) {
                        case '"' -> {
                            stringBuilder.append('\"'); states.pop();}
                        case '\\' -> {
                            stringBuilder.append('\\'); states.pop();}
                        case '/' -> {
                            stringBuilder.append('/'); states.pop();}
                        case 'b' -> {
                            stringBuilder.append('b'); states.pop();}
                        case 'f' -> {
                            stringBuilder.append('f'); states.pop();}
                        case 'n' -> {
                            stringBuilder.append('\n'); states.pop();}
                        case 'r' -> {
                            stringBuilder.append('\r'); states.pop();}
                        case 't' -> {
                            stringBuilder.append('\t'); states.pop();}
//                        case 'u' -> {state = State.READING_STRING_ESCAPED_U;}
                    }
                }
//                case READING_STRING_ESCAPED_U -> {
//                    if (isHex(c)) {
//                        state = State.READING_STRING_ESCAPED_U_X;
//                    } else {
//                        throw new RuntimeException();
//                    }
//                }
//                case READING_STRING_ESCAPED_U_X -> {
//                    if (isHex(c)) {
//                        uXXXX.append(c);
//                        state = State.READING_STRING_ESCAPED_U_XX;
//                    } else {
//                        throw new RuntimeException();
//                    }
//                }
//                case READING_STRING_ESCAPED_U_XX -> {
//                    if (isHex(c)) {
//                        uXXXX.append(c);
//                        state = State.READING_STRING_ESCAPED_U_XXX;
//                    } else {
//                        throw new RuntimeException();
//                    }
//                }
//                case READING_STRING_ESCAPED_U_XXX -> {
//                    if (isHex(c)) {
//                        uXXXX.append(c);
//                        final char cXXXX = (char) Integer.parseInt(uXXXX.toString(), 16);
//                        stringBuilder.append(cXXXX);
//                        uXXXX.setLength(0);
//                        state = State.READING_STRING;
//                    } else {
//                        throw new RuntimeException();
//                    }
//                }
//                case N -> {
//                    switch (c) {
//                        case 'u' -> state = State.NU;
//                        default -> throw new RuntimeException();
//                    }
//                }
//                case NU -> {
//                    switch (c) {
//                        case 'l' -> state = State.NUL;
//                        default -> throw new RuntimeException();
//                    }
//                }
//                case NUL -> {
//                    switch (c) {
//                        case 'l' -> {
//                            System.out.println("null");
//                            state = State.READY;
//                        }
//                        default -> throw new RuntimeException();
//                    }
//                }
//                case READING_OBJ -> {
//                    objStack.add(new HashMap<>());
//                    stateStack.push(State.READ_ALL, State.READING_STRING);
//                    switch (c) {
//                        case ' ' -> {}
//                        case '"' -> {state = State.READING_STRING;}
//                        default -> throw new RuntimeException();
//                    }
//                }

                case ARR_NEXT -> {
                    states.pop();
                    collapseList();
                    switch (c) {
                        case ',' ->
                                plan(State.WS, State.ANY, State.WS, State.ARR_NEXT);
                        default -> i--;

                    }
                }
                case ARR -> {
                    switch (c) {
                        case ']' -> states.pop();
                        default -> {
                            i--;
                            plan(State.WS, State.ANY, State.WS, State.ARR_NEXT);
                        }
                    }
                }
                case OBJ_NEXT -> {
                    states.pop();
                    collapseMap();
                    switch (c) {
                        case ',' ->
                                plan(State.WS, State.QUOTE, State.STR, State.WS, State.COLON, State.WS, State.ANY, State.WS, State.OBJ_NEXT);
                        default -> i--;
                    }
                }
                case COLON -> {
                    switch (c) {
                        case ':' -> states.pop();
                        default -> throw new RuntimeException(String.format("not a colon: %s", c));
                    }
                }
                case QUOTE -> {
                    switch (c) {
                        case '"' -> states.pop();
                        default -> throw new RuntimeException(String.format("not a quote: %s", c));
                    }
                }
                case OBJ -> {
                    switch (c) {
                        case '}' -> states.pop();
                        default -> {
                            i--;
                            plan(State.QUOTE, State.STR, State.WS, State.COLON, State.WS, State.ANY, State.WS, State.OBJ_NEXT);
                        }
                    }
                }
                case DONE -> {
                    //states.pop();
                    break;
                }
                default -> throw new RuntimeException(String.format("unknown state: %s", states.peek()));
//                case READING_ARR_ITEM_READ_NEXT -> {
//                    switch (c) {
//                        case ' ' -> {}
//                        case ']' -> states.pop();
//                        case ',' -> {
//                            states.push(State.READING_ARR_ITEM_READ);
//                            states.push(State.READ_ANY);
//                        }
//                    }
//                }
//                case READING_ARR_ITEM_READ -> {
//                    commit();
//                    i--;
//                    states.pop();
//                    states.push(State.READING_ARR_ITEM_READ_NEXT);
//
//                }

            }
        }
    }

    private Object complete() {
        return null;
    }

    public Object parse() throws IOException {
        while (!eof) {
            fillBuffer();
//            printBuffer();
            parseBuf();
        }
        return complete();
    }

    public static void main(String[] args) throws IOException {
//        final Parser p = new Parser(new StringReader("[ \"abc\" , \"xyz\" , [\"ccc\" , \"aaa\" ] ]"));
//        final Parser p = new Parser(new StringReader("  \"absdfsdfsdfc\"  "));
//        final Parser p = new Parser(new StringReader("   "));
//        final Parser p = new Parser(new StringReader("[ \"abc\" , \"xyz\", [ \"a\",  [ \"a\", \"a\", \"a\"  ], \"a\", \"a\"  ], \"1\" , \"2\", \"3\" ]"));

        final Parser p = new Parser(new StringReader(" {\n" +
                "    \"macros\": [\n" +
                "        {\n" +
                "            \"id\": 1,\n" +
                "            \"name\": \"TIMESTAMP\",\n" +
                "            \"categoryname\": \"Generic Macros\",\n" +
                "            \"categoryindex\": 2,\n" +
                "            \"datatype\": \"string\",\n" +
                "            \"type\": \"string\",\n" +
                "            \"introversion\": \"4.0\",\n" +
                "            \"support\": \"Required\",\n" +
                "            \"contexts\": \"All tracking pixels,VAST request URIs\",\n" +
                "            \"format\": \"{YYYY-MM-DD}T{HH:MM:SS}.{mmm}{+/-}{ZONEOFFSET}\",\n" +
                "            \"description\": \"The date and time at which the URI using this macro is accessed. Used wherever a time stamp is needed, the macro is replaced with the date and time using the formatting conventions of ISO 8601.\\nTo add milliseconds, use the convention .mmm at the end of the time provided and before any time zone indicator.\",\n" +
                "            \"descriptionformatted\": \"The date and time at which the URI using this macro is accessed. Used wherever a time stamp is needed, the macro is replaced with the date and time using the formatting conventions of ISO 8601. <p>To add milliseconds, use the convention <code>.mmm</code> at the end of the time provided and before any time zone indicator.</p>\",\n" +
                "            \"example\": \"January 17, 2016 at 8:15:07 and 127 milliseconds, Eastern Time would be formatted as follows:<br>Unencoded: <code>2016-01-17T8:15:07.127-05</code><br>Encoded: <code>2016-01-17T8%3A15%3A07.127-05</code>\"\n" +
                "        },\n" +
                "        {\n" +
                "            \"id\": 2,\n" +
                "            \"name\": \"CACHEBUSTING\",\n" +
                "            \"categoryname\": \"Generic Macros\",\n" +
                "            \"categoryindex\": 2,\n" +
                "            \"datatype\": \"integer\",\n" +
                "            \"type\": \"integer\",\n" +
                "            \"introversion\": \"3.0\",\n" +
                "            \"support\": \"Required\",\n" +
                "            \"contexts\": \"All tracking pixels,VAST request URIs\",\n" +
                "            \"description\": \"Random 8-digit integer.\",\n" +
                "            \"descriptionformatted\": \"Random 8-digit integer.\",\n" +
                "            \"example\": \"<code>12345678</code>\"\n" +
                "        },\n" +
                "        {\n" +
                "            \"id\": 3,\n" +
                "            \"name\": \"CONTENTPLAYHEAD\",\n" +
                "            \"categoryname\": \"Ad Break Info\",\n" +
                "            \"categoryindex\": 3,\n" +
                "            \"datatype\": \"string\",\n" +
                "            \"type\": \"timecode\",\n" +
                "            \"introversion\": \"3.0\",\n" +
                "            \"support\": \"<span style=\\\"color: red;font-weight: 700;\\\">Deprecated in VAST 4.1</span>, replaced by <code>[ADPLAYHEAD]</code> and <code>[MEDIAPLAYHEAD]</code>\",\n" +
                "            \"contexts\": \"All tracking pixels,VAST request URIs\",\n" +
                "            \"format\": \"{HH:MM:SS.mmm}\",\n" +
                "            \"description\": \"The current time offset of the video or audio content.\",\n" +
                "            \"descriptionformatted\": \"The current time offset of the video or audio content.\",\n" +
                "            \"example\": \"Unencoded: <code>00:05:21.123</code><br>Encoded: <code>00%3A05%3A21.123</code>\"\n" +
                "        },\n" +
                "        {\n" +
                "            \"id\": 4,\n" +
                "            \"name\": \"MEDIAPLAYHEAD\",\n" +
                "            \"categoryname\": \"Ad Break Info\",\n" +
                "            \"categoryindex\": 3,\n" +
                "            \"datatype\": \"string\",\n" +
                "            \"type\": \"timecode\",\n" +
                "            \"introversion\": \"4.1\",\n" +
                "            \"support\": \"Optional\",\n" +
                "            \"contexts\": \"All tracking pixels,VAST request URIs\",\n" +
                "            \"format\": \"{HH:MM:SS.mmm}\",\n" +
                "            \"description\": \"Playhead position of the video or audio content (NOT the ad creative). \\nNot relevant for out-stream ads.\",\n" +
                "            \"descriptionformatted\": \"Playhead position of the video or audio content (NOT the ad creative). Not relevant for out-stream ads.\",\n" +
                "            \"example\": \"Unencoded: <code>00:05:21.123</code><br>Encoded: <code>00%3A05%3A21.123</code>\"\n" +
                "        },\n" +
                "        {\n" +
                "            \"id\": 5,\n" +
                "            \"name\": \"BREAKPOSITION\",\n" +
                "            \"categoryname\": \"Ad Break Info\",\n" +
                "            \"categoryindex\": 3,\n" +
                "            \"datatype\": \"integer\",\n" +
                "            \"type\": \"integer\",\n" +
                "            \"introversion\": \"4.1\",\n" +
                "            \"support\": \"Optional\",\n" +
                "            \"contexts\": \"All tracking pixels,VAST request URIs\",\n" +
                "            \"values\": \"see:BREAKPOSITION_values_worksheet\",\n" +
                "            \"description\": \"Indicates the position of the ad break within the underlying video/audio content that the ad is playing within.\",\n" +
                "            \"descriptionformatted\": \"Indicates the position of the ad break within the underlying video/audio content that the ad is playing in.\",\n" +
                "            \"example\": \"<code>2</code>\"\n" +
                "        },\n" +
                "        {\n" +
                "            \"id\": 6,\n" +
                "            \"name\": \"BLOCKEDADCATEGORIES\",\n" +
                "            \"categoryname\": \"Ad Break Info\",\n" +
                "            \"categoryindex\": 3,\n" +
                "            \"datatype\": \"Array<string>\",\n" +
                "            \"type\": \"Array<string>\",\n" +
                "            \"introversion\": \"4.1\",\n" +
                "            \"support\": \"Optional\",\n" +
                "            \"contexts\": \"VAST request URIs\",\n" +
                "            \"format\": \"IAB{N}-{N}\",\n" +
                "            \"description\": \"List of blocked ad categories. Encoded with IABN-N values as specified in the \\\"Content Categories\\\" list of AdCOM 1.0.\\nValues must be taken from the BlockedCategory element (3.19.2) in Wrapper ad elements.\",\n" +
                "            \"descriptionformatted\": \"List of blocked ad categories. Encoded with <code>IABN-N</code> values as specified in the \\\"Content Categories\\\" list of AdCOM 1.0.<br>Values must be taken from the BlockedCategory element (3.19.2) in Wrapper ad elements.\",\n" +
                "            \"example\": \"<code>IAB1-6,IAB1-7</code>\"\n" +
                "        },\n" +
                "        {\n" +
                "            \"id\": 7,\n" +
                "            \"name\": \"ADCATEGORIES\",\n" +
                "            \"categoryname\": \"Ad Break Info\",\n" +
                "            \"categoryindex\": 3,\n" +
                "            \"datatype\": \"Array<string>\",\n" +
                "            \"type\": \"Array<string>\",\n" +
                "            \"introversion\": \"4.1\",\n" +
                "            \"support\": \"Optional\",\n" +
                "            \"contexts\": \"VAST request URIs\",\n" +
                "            \"format\": \"IAB{N}-{N}\",\n" +
                "            \"description\": \"List of desired ad categories. Encoded with IABN-N values as specified in the \\\"Content Categories\\\" list of AdCOM 1.0.\",\n" +
                "            \"descriptionformatted\": \"List of desired ad categories. Encoded with <code>IABN-N</code> values as specified in the \\\"Content Categories\\\" list of AdCOM 1.0.\",\n" +
                "            \"example\": \"<code>IAB1-6,IAB1-7</code>\"\n" +
                "        },\n" +
                "        {\n" +
                "            \"id\": 8,\n" +
                "            \"name\": \"ADCOUNT\",\n" +
                "            \"categoryname\": \"Ad Break Info\",\n" +
                "            \"categoryindex\": 3,\n" +
                "            \"datatype\": \"integer\",\n" +
                "            \"type\": \"integer\",\n" +
                "            \"introversion\": \"4.1\",\n" +
                "            \"support\": \"Optional\",\n" +
                "            \"contexts\": \"All tracking pixels,VAST request URIs\",\n" +
                "            \"description\": \"For VAST requests: the number of ads expected by the player\\nFor tracking pixels: the number of <InLine> ads played within the current chain or tree of VASTs, including the executing one. That is, this value starts at 1 and increments for each video played, whether it was pulled from a Pod, buffet, nested Pod, etc. In standard non-Pod VAST responses with a single <InLine> ad, this value is always 1.\",\n" +
                "            \"descriptionformatted\": \"For VAST requests: the number of ads expected by the player<p>For tracking pixels: the number of <code><InLine></code> ads played within the current chain or tree of VASTs, including the executing one. That is, this value starts at 1 and increments for each video played, whether it was pulled from a Pod, buffet, nested Pod, etc. In standard non-Pod VAST responses with a single <code><InLine></code> ad, this value is always 1.</p>\",\n" +
                "            \"example\": \"<code>2</code>\"\n" +
                "        },\n" +
                "        {\n" +
                "            \"id\": 9,\n" +
                "            \"name\": \"TRANSACTIONID\",\n" +
                "            \"categoryname\": \"Ad Break Info\",\n" +
                "            \"categoryindex\": 3,\n" +
                "            \"datatype\": \"string\",\n" +
                "            \"type\": \"UUID\",\n" +
                "            \"introversion\": \"4.1\",\n" +
                "            \"support\": \"Optional\",\n" +
                "            \"contexts\": \"All tracking pixels,VAST request URIs\",\n" +
                "            \"format\": \"UUID\",\n" +
                "            \"description\": \"An identifier used to correlate a chain of ad requests from the origination (supply) end. This ID is generated by the initiating player and subsequent ad requests along the chain must pass the same ID along. This ID must be a UUID. Note that this is unique to the initial request, even if there are multiple ads in the response, as in VMAP and VAST Ad Pod cases.\",\n" +
                "            \"descriptionformatted\": \"An identifier used to correlate a chain of ad requests from the origination (supply) end. This ID is generated by the initiating player and subsequent ad requests along the chain must pass the same ID along. This ID must be a UUID. Note that this is unique to the initial request, even if there are multiple ads in the response, as in VMAP and VAST Ad Pod cases.\",\n" +
                "            \"example\": \"<code>123e4567-e89b-12d3-a456-426655440000</code>\"\n" +
                "        },\n" +
                "        {\n" +
                "            \"id\": 10,\n" +
                "            \"name\": \"PLACEMENTTYPE\",\n" +
                "            \"categoryname\": \"Ad Break Info\",\n" +
                "            \"categoryindex\": 3,\n" +
                "            \"datatype\": \"integer\",\n" +
                "            \"type\": \"integer\",\n" +
                "            \"introversion\": \"4.1\",\n" +
                "            \"support\": \"Optional\",\n" +
                "            \"contexts\": \"All tracking pixels,VAST request URIs\",\n" +
                "            \"description\": \"Indicates the type of ad placement. Refer to the Placement Subtypes - Video list in AdCOM.\",\n" +
                "            \"descriptionformatted\": \"Indicates the type of ad placement. Refer to the Placement Subtypes - Video list in AdCOM.\",\n" +
                "            \"example\": \"<code>1</code>\"\n" +
                "        },\n" +
                "        {\n" +
                "            \"id\": 11,\n" +
                "            \"name\": \"ADTYPE\",\n" +
                "            \"categoryname\": \"Ad Break Info\",\n" +
                "            \"categoryindex\": 3,\n" +
                "            \"datatype\": \"string\",\n" +
                "            \"type\": \"string\",\n" +
                "            \"introversion\": \"4.1\",\n" +
                "            \"support\": \"Optional\",\n" +
                "            \"contexts\": \"All tracking pixels,VAST request URIs\",\n" +
                "            \"description\": \"Indicates whether the ad’s intended use case was video, audio, or hybrid, as defined in the adType attribute of the VAST <Ad> element.\",\n" +
                "            \"descriptionformatted\": \"Indicates whether the ad’s intended use case was video, audio, or hybrid, as defined in the <code>adType</code> attribute of the VAST <code><Ad></code> element.\",\n" +
                "            \"example\": \"<code>video</code>\"\n" +
                "        },\n" +
                "        {\n" +
                "            \"id\": 12,\n" +
                "            \"name\": \"UNIVERSALADID\",\n" +
                "            \"categoryname\": \"Ad Break Info\",\n" +
                "            \"categoryindex\": 3,\n" +
                "            \"datatype\": \"Array<string>\",\n" +
                "            \"type\": \"Array<string>\",\n" +
                "            \"introversion\": \"4.1\",\n" +
                "            \"support\": \"Optional\",\n" +
                "            \"contexts\": \"All tracking pixels\",\n" +
                "            \"format\": \"{registryID}{space}{idvalue}\",\n" +
                "            \"description\": \"Indicates the creative, using UniversalAdId values described in section 3.7.1.\\nFormat: registryID idvalue - with a space separating registryID and value.\\nMultiple UniversalAdIds can be supported with a comma-separating them.\",\n" +
                "            \"descriptionformatted\": \"Indicates the creative, using <code>UniversalAdId</code> values described in section 3.7.1.\",\n" +
                "            \"example\": \"Unencoded: <code>ad-id.org CNPA0484000H</code><br>Encoded: <code>ad-id.org%20CNPA0484000H</code>\"\n" +
                "        },\n" +
                "        {\n" +
                "            \"id\": 13,\n" +
                "            \"name\": \"BREAKMAXDURATION\",\n" +
                "            \"categoryname\": \"Ad Break Info\",\n" +
                "            \"categoryindex\": 3,\n" +
                "            \"datatype\": \"integer\",\n" +
                "            \"type\": \"integer\",\n" +
                "            \"introversion\": \"4.2\",\n" +
                "            \"support\": \"Optional\",\n" +
                "            \"contexts\": \"All tracking pixels,VAST request URIs\",\n" +
                "            \"description\": \"Indicates the maximum length allowed for the ad break in seconds.\",\n" +
                "            \"descriptionformatted\": \"Indicates the maximum length allowed for the ad break in seconds.\",\n" +
                "            \"example\": \"<code>30</code> - 30 seconds max total length allowed in the break.\"\n" +
                "        },\n" +
                "        {\n" +
                "            \"id\": 14,\n" +
                "            \"name\": \"BREAKMAXADS\",\n" +
                "            \"categoryname\": \"Ad Break Info\",\n" +
                "            \"categoryindex\": 3,\n" +
                "            \"datatype\": \"integer\",\n" +
                "            \"type\": \"integer\",\n" +
                "            \"introversion\": \"4.2\",\n" +
                "            \"support\": \"Optional\",\n" +
                "            \"contexts\": \"All tracking pixels,VAST request URIs\",\n" +
                "            \"description\": \"Indicates the maximum number of ads allowed in the ad break.\",\n" +
                "            \"descriptionformatted\": \"Indicates the maximum number of ads allowed in the ad break.\",\n" +
                "            \"example\": \"<code>2</code> - two ads max in the break.\"\n" +
                "        },\n" +
                "        {\n" +
                "            \"id\": 15,\n" +
                "            \"name\": \"BREAKMINADLENGTH\",\n" +
                "            \"categoryname\": \"Ad Break Info\",\n" +
                "            \"categoryindex\": 3,\n" +
                "            \"datatype\": \"integer\",\n" +
                "            \"type\": \"integer\",\n" +
                "            \"introversion\": \"4.2\",\n" +
                "            \"support\": \"Optional\",\n" +
                "            \"contexts\": \"All tracking pixels,VAST request URIs\",\n" +
                "            \"description\": \"Indicates the minimum length allowed for any individual ad in the ad break in seconds.\",\n" +
                "            \"descriptionformatted\": \"Indicates the minimum length allowed for any individual ad in the ad break in seconds.\",\n" +
                "            \"example\": \"<code>5</code> - five seconds is the min duration of any ad allowed in the break.\"\n" +
                "        },\n" +
                "        {\n" +
                "            \"id\": 16,\n" +
                "            \"name\": \"BREAKMAXADLENGTH\",\n" +
                "            \"categoryname\": \"Ad Break Info\",\n" +
                "            \"categoryindex\": 3,\n" +
                "            \"datatype\": \"integer\",\n" +
                "            \"type\": \"integer\",\n" +
                "            \"introversion\": \"4.2\",\n" +
                "            \"support\": \"Optional\",\n" +
                "            \"contexts\": \"All tracking pixels,VAST request URIs\",\n" +
                "            \"description\": \"Indicates the maximum length allowed for any individual ad in the ad break in seconds.\",\n" +
                "            \"descriptionformatted\": \"Indicates the maximum length allowed for any individual ad in the ad break in seconds.\",\n" +
                "            \"example\": \"<code>15</code> - fifteen seconds is the maximum duration of any ad allowed in the break.\"\n" +
                "        },\n" +
                "        {\n" +
                "            \"id\": 17,\n" +
                "            \"name\": \"IFA\",\n" +
                "            \"categoryname\": \"Client Info\",\n" +
                "            \"categoryindex\": 4,\n" +
                "            \"datatype\": \"string\",\n" +
                "            \"type\": \"string\",\n" +
                "            \"introversion\": \"4.1\",\n" +
                "            \"support\": \"Optional\",\n" +
                "            \"contexts\": \"All tracking pixels,VAST request URIs\",\n" +
                "            \"format\": \"UUID\",\n" +
                "            \"description\": \"A resettable advertising ID from a device-specific advertising ID scheme, such as Apple’s ID for Advertisers or Android’s Advertising ID in UUID format or based on the IAB Tech Lab’s Guidelines for IFA on OTT platforms.\",\n" +
                "            \"descriptionformatted\": \"A resettable advertising ID from a device-specific advertising ID scheme, such as Apple’s ID for Advertisers or Android’s Advertising ID in UUID format or based on the <a href=\\\"https://iabtechlab.com/standards/guidelines-identifier-advertising-over-the-top-platforms/\\\">IAB Tech Lab’s Guidelines for IFA on OTT platforms</a>.\",\n" +
                "            \"example\": \"<code>123e4567-e89b-12d3-a456-426655440000</code>\"\n" +
                "        },\n" +
                "        {\n" +
                "            \"id\": 18,\n" +
                "            \"name\": \"IFATYPE\",\n" +
                "            \"categoryname\": \"Client Info\",\n" +
                "            \"categoryindex\": 4,\n" +
                "            \"datatype\": \"string\",\n" +
                "            \"type\": \"string\",\n" +
                "            \"introversion\": \"4.1\",\n" +
                "            \"support\": \"Optional\",\n" +
                "            \"contexts\": \"All tracking pixels,VAST request URIs\",\n" +
                "            \"description\": \"String value indicating the type of IFA included in the [IFA] macro. More details in the IAB Tech Lab’s Guidelines for IFA on OTT platforms.\",\n" +
                "            \"descriptionformatted\": \"String value indicating the type of IFA included in the <code>[IFA]</code> macro. More details in the <a href=\\\"https://iabtechlab.com/standards/guidelines-identifier-advertising-over-the-top-platforms/\\\">IAB Tech Lab’s Guidelines for IFA on OTT platforms</a>.\",\n" +
                "            \"example\": \"<code>1rida</code>\"\n" +
                "        },\n" +
                "        {\n" +
                "            \"id\": 19,\n" +
                "            \"name\": \"CLIENTUA\",\n" +
                "            \"categoryname\": \"Client Info\",\n" +
                "            \"categoryindex\": 4,\n" +
                "            \"datatype\": \"string\",\n" +
                "            \"type\": \"string\",\n" +
                "            \"introversion\": \"4.1\",\n" +
                "            \"support\": \"Optional\",\n" +
                "            \"contexts\": \"All tracking pixels,VAST request URIs\",\n" +
                "            \"format\": \"{player name}/{player version}{space}{plugin name}/{plugin version}\",\n" +
                "            \"description\": \"An identifier of the player and VAST client used. This will allow creative providers to identify the client code used to process and render video creatives.\\nIf player name is not available, use \\\"unknown\\\".\",\n" +
                "            \"descriptionformatted\": \"An identifier of the player and VAST client used. This will allow creative providers to identify the client code used to process and render video creatives. <br>If player name is not available, use \\\"unknown\\\".\",\n" +
                "            \"example\": \"Unencoded:<code>MyPlayer/7.1 MyPlayerVastPlugin/1.1.2</code><br>Encoded:<code>MyPlayer%2F7.1%20MyPlayerVastPlugin%2F1.1.2</code><p>Unencoded:<code>unknown MyPlayerVastPlugin/1.1.2</code><br>Encoded:<code>unknown%20MyPlayerVastPlugin%2F1.1.2</code></p>\"\n" +
                "        },\n" +
                "        {\n" +
                "            \"id\": 20,\n" +
                "            \"name\": \"SERVERUA\",\n" +
                "            \"categoryname\": \"Client Info\",\n" +
                "            \"categoryindex\": 4,\n" +
                "            \"datatype\": \"string\",\n" +
                "            \"type\": \"string\",\n" +
                "            \"introversion\": \"4.1\",\n" +
                "            \"support\": \"Optional\",\n" +
                "            \"contexts\": \"All tracking pixels,VAST request URIs\",\n" +
                "            \"format\": \"{service name}/{version} ({URL to vendor info})\",\n" +
                "            \"description\": \"User-Agent of the server making the request on behalf of a client.\\nOnly relevant when another device (server) is making the request on behalf of that client.\\n\\nThe goal is to allow ad servers to identify who is making the request, so don’t use generic HTTP server names like Apache, but rather identify the company and product or service making the request.\",\n" +
                "            \"descriptionformatted\": \"User-Agent of the server making the request on behalf of a client. <p>Only relevant when another device (server) is making the request on behalf of that client.</p><p>The goal is to allow ad servers to identify who is making the request, so don’t use generic HTTP server names like <code>Apache</code>, but rather identify the company and product or service making the request.</p>\",\n" +
                "            \"example\": \"Unencoded:<code>MyServer/3.0 (+https://myserver.com/contact)</code><br>Encoded:<code>MyServer%2F3.0%20(%2Bhttps%3A%2F%2Fmyserver.com%2Fcontact)</code><p>Unencoded:<code>AdsBot-Google (+http://www.google.com/adsbot.html)</code><br>Encoded:<code>AdsBot-Google%20(%2Bhttp%3A%2F%2Fwww.google.com%2Fadsbot.html)</code></p>\"\n" +
                "        },\n" +
                "        {\n" +
                "            \"id\": 21,\n" +
                "            \"name\": \"DEVICEUA\",\n" +
                "            \"categoryname\": \"Client Info\",\n" +
                "            \"categoryindex\": 4,\n" +
                "            \"datatype\": \"string\",\n" +
                "            \"type\": \"string\",\n" +
                "            \"introversion\": \"4.1\",\n" +
                "            \"support\": \"Optional\",\n" +
                "            \"contexts\": \"All tracking pixels,VAST request URIs\",\n" +
                "            \"description\": \"User-Agent of the device that is rendering the ad to the end user.\\nOnly relevant when another device (server) is making the request on behalf of that client.\",\n" +
                "            \"descriptionformatted\": \"User-Agent of the device that is rendering the ad to the end user. <p>Only relevant when another device (server) is making the request on behalf of that client.</p>\",\n" +
                "            \"example\": \" Unencoded:<code>Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/51.0.2704.103 Safari/537.36</code><br>Encoded:<code>Mozilla%2F5.0%20(X11%3B%20Linux%20x86_64)%20AppleWebKit%2F537.36%20(KHTML%2C%20like%20Gecko)%20Chrome%2F51.0.2704.103%20Safari%2F537.36</code>\"\n" +
                "        },\n" +
                "        {\n" +
                "            \"id\": 22,\n" +
                "            \"name\": \"SERVERSIDE\",\n" +
                "            \"categoryname\": \"Client Info\",\n" +
                "            \"categoryindex\": 4,\n" +
                "            \"datatype\": \"integer\",\n" +
                "            \"type\": \"integer\",\n" +
                "            \"introversion\": \"4.1\",\n" +
                "            \"support\": \"Optional\",\n" +
                "            \"contexts\": \"All tracking pixels,VAST request URIs\",\n" +
                "            \"values\": \"see:SERVERSIDE_values_worksheet\",\n" +
                "            \"description\": \"Value indicating if a URL is requested from a client device or a server. This value may be set differently on the request versus tracking URLs, as the request may be made from a server (value of 1 or 2) while tracking URLs may be fired from the client (value of 0).\\n\\n0 is the default value if macro is missing.\",\n" +
                "            \"descriptionformatted\": \" Value indicating if a URL is requested from a client device or a server. This value may be set differently on the request versus tracking URLs, as the request may be made from a server (value of <code>1</code> or <code>2</code>) while tracking URLs may be fired from the client (value of <code>0</code>). <code>0</code> is the default value if macro is missing.\",\n" +
                "            \"example\": \"<code>1</code>\"\n" +
                "        },\n" +
                "        {\n" +
                "            \"id\": 23,\n" +
                "            \"name\": \"DEVICEIP\",\n" +
                "            \"categoryname\": \"Client Info\",\n" +
                "            \"categoryindex\": 4,\n" +
                "            \"datatype\": \"string\",\n" +
                "            \"type\": \"string\",\n" +
                "            \"introversion\": \"4.1\",\n" +
                "            \"support\": \"Optional\",\n" +
                "            \"contexts\": \"All tracking pixels,VAST request URIs\",\n" +
                "            \"description\": \"IP address of the device that is rendering the ad to the end user.\\nOnly relevant when another device (server) is making the request on behalf of that client.\",\n" +
                "            \"descriptionformatted\": \"IP address of the device that is rendering the ad to the end user.<p>Only relevant when another device (server) is making the request on behalf of that client.</p>\",\n" +
                "            \"example\": \"IPv6 unencoded: <code>2001:0db8:85a3:0000:0000:8a2e:0370:7334</code><br>IPv6 encoded: <code>2001%3A0db8%3A85a3%3A0000%3A0000%3A8a2e%3A0370%3A7334</code>\"\n" +
                "        },\n" +
                "        {\n" +
                "            \"id\": 24,\n" +
                "            \"name\": \"LATLONG\",\n" +
                "            \"categoryname\": \"Client Info\",\n" +
                "            \"categoryindex\": 4,\n" +
                "            \"datatype\": \"Array<float>(2)\",\n" +
                "            \"type\": \"Array<number>(2)\",\n" +
                "            \"introversion\": \"4.1\",\n" +
                "            \"support\": \"Optional\",\n" +
                "            \"contexts\": \"All tracking pixels,VAST request URIs\",\n" +
                "            \"description\": \"Mobile detected geolocation info of the end user; latitude and longitude separated by a coma.\",\n" +
                "            \"descriptionformatted\": \"Mobile detected geolocation info of the end user; latitude and longitude separated by a coma.\",\n" +
                "            \"example\": \"<code>51.004703,3.754806</code>\"\n" +
                "        },\n" +
                "        {\n" +
                "            \"id\": 25,\n" +
                "            \"name\": \"DOMAIN\",\n" +
                "            \"categoryname\": \"Publisher Info\",\n" +
                "            \"categoryindex\": 5,\n" +
                "            \"datatype\": \"string\",\n" +
                "            \"type\": \"string\",\n" +
                "            \"introversion\": \"4.1\",\n" +
                "            \"support\": \"Optional\",\n" +
                "            \"contexts\": \"All tracking pixels,VAST request URIs\",\n" +
                "            \"description\": \"Domain of the top level page where the end user will view the ad.\",\n" +
                "            \"descriptionformatted\": \"Domain of the top level page where the end user will view the ad.\",\n" +
                "            \"example\": \"<code>www.mydomain.com</code>\"\n" +
                "        },\n" +
                "        {\n" +
                "            \"id\": 26,\n" +
                "            \"name\": \"PAGEURL\",\n" +
                "            \"categoryname\": \"Publisher Info\",\n" +
                "            \"categoryindex\": 5,\n" +
                "            \"datatype\": \"string\",\n" +
                "            \"type\": \"string\",\n" +
                "            \"introversion\": \"4.1\",\n" +
                "            \"support\": \"Required if OM for Web is supported, otherwise Optional.\",\n" +
                "            \"contexts\": \"All tracking pixels,VAST request URIs\",\n" +
                "            \"description\": \"The full URL of the top level page where the end user will view the ad. Where required and applicable, but unknown or unavailable, -1 or -2 must be set, as described in 6.1. When required and not applicable (such as in an app), 0 must be set.\",\n" +
                "            \"descriptionformatted\": \"The full URL of the top level page where the end user will view the ad. Where required and applicable, but unknown or unavailable, <code>-1</code> or <code>-2</code> must be set, as described in 6.1. When required and not applicable (such as in an app), <code>0</code> must be set.\",\n" +
                "            \"example\": \"Unencoded:<code>https://www.mydomain.com/article/page</code><br>Encoded:<code>https%3A%2F%2Fwww.mydomain.com%2Farticle%2Fpage</code>\"\n" +
                "        },\n" +
                "        {\n" +
                "            \"id\": 27,\n" +
                "            \"name\": \"APPBUNDLE\",\n" +
                "            \"categoryname\": \"Publisher Info\",\n" +
                "            \"categoryindex\": 5,\n" +
                "            \"datatype\": \"string\",\n" +
                "            \"type\": \"string\",\n" +
                "            \"introversion\": \"4.1\",\n" +
                "            \"support\": \"Required if OM for App is supported, otherwise Optional.\",\n" +
                "            \"contexts\": \"All tracking pixels,VAST request URIs\",\n" +
                "            \"description\": \"For app ads, a platform-specific application identifier, bundle or package name and should not be an app store ID such as iTunes store ID. Where required and applicable, but unknown or unavailable, -1 or -2 must be set, as described in 6.1. When required and not applicable (such as in an app), 0 must be set.\",\n" +
                "            \"descriptionformatted\": \"For app ads, a platform-specific application identifier, bundle or package name and should not be an app store ID such as iTunes store ID.  Where required and applicable, but unknown or unavailable, <code>-1</code> or <code>-2</code> must be set, as described in 6.1. When required and not applicable (such as in an app), <code>0</code> must be set.\",\n" +
                "            \"example\": \"<code>com.example.myapp</code>\"\n" +
                "        },\n" +
                "        {\n" +
                "            \"id\": 28,\n" +
                "            \"name\": \"VASTVERSIONS\",\n" +
                "            \"categoryname\": \"Capabilities Info\",\n" +
                "            \"categoryindex\": 6,\n" +
                "            \"datatype\": \"Array<integer>\",\n" +
                "            \"type\": \"Array<integer>\",\n" +
                "            \"introversion\": \"4.1\",\n" +
                "            \"support\": \"Optional\",\n" +
                "            \"contexts\": \"VAST request URIs\",\n" +
                "            \"description\": \"List of VAST versions supported by the player. Values are defined in the AdCOM 1.0 \\\"Creative Subtypes\\\" list. The relevant IDs have been copied here for convenience.\\n11 for VAST 4.1\\n12 for VAST 4.1 Wrapper\",\n" +
                "            \"descriptionformatted\": \"List of VAST versions supported by the player. Values are defined in the AdCOM 1.0 \\\"Creative Subtypes\\\" list. The relevant IDs have been copied here for convenience.<p></p><code>11</code> for VAST 4.1<br><code>12</code> for VAST 4.1 Wrapper\",\n" +
                "            \"example\": \"<code>2,3,5,6,7,8,11</code>\"\n" +
                "        },\n" +
                "        {\n" +
                "            \"id\": 29,\n" +
                "            \"name\": \"APIFRAMEWORKS\",\n" +
                "            \"categoryname\": \"Capabilities Info\",\n" +
                "            \"categoryindex\": 6,\n" +
                "            \"datatype\": \"Array<integer>\",\n" +
                "            \"type\": \"Array<number>\",\n" +
                "            \"introversion\": \"4.1\",\n" +
                "            \"support\": \"Optional\",\n" +
                "            \"contexts\": \"VAST request URIs\",\n" +
                "            \"description\": \"ist of frameworks supported by the player. Values are defined in the AdCOM 1.0 \\\"API Frameworks\\\" list.\",\n" +
                "            \"descriptionformatted\": \"List of frameworks supported by the player. Values are defined in the AdCOM 1.0 \\\"API Frameworks\\\" list.\",\n" +
                "            \"example\": \"<code>2,7</code>\"\n" +
                "        },\n" +
                "        {\n" +
                "            \"id\": 30,\n" +
                "            \"name\": \"EXTENSIONS\",\n" +
                "            \"categoryname\": \"Capabilities Info\",\n" +
                "            \"categoryindex\": 6,\n" +
                "            \"datatype\": \"Array<string>\",\n" +
                "            \"type\": \"Array<string>\",\n" +
                "            \"introversion\": \"4.1\",\n" +
                "            \"support\": \"Optional\",\n" +
                "            \"contexts\": \"VAST request URIs\",\n" +
                "            \"description\": \"List of VAST Extensions type attribute values that the player / client supports. Can be used to indicate support for the OMID AdVerifications extension, proprietary extensions, or future standardized extensions.\",\n" +
                "            \"descriptionformatted\": \"List of VAST Extensions type attribute values that the player / client supports. Can be used to indicate support for the OMID AdVerifications extension, proprietary extensions, or future standardized extensions.\",\n" +
                "            \"example\": \"<code>AdVerifications,extensionA,extensionB</code>\"\n" +
                "        },\n" +
                "        {\n" +
                "            \"id\": 31,\n" +
                "            \"name\": \"VERIFICATIONVENDORS\",\n" +
                "            \"categoryname\": \"Capabilities Info\",\n" +
                "            \"categoryindex\": 6,\n" +
                "            \"datatype\": \"Array<string>\",\n" +
                "            \"type\": \"Array<string>\",\n" +
                "            \"introversion\": \"4.1\",\n" +
                "            \"support\": \"Optional\",\n" +
                "            \"contexts\": \"VAST request URIs\",\n" +
                "            \"description\": \"List of VAST Verification vendor attribute values that the player / client supports.\",\n" +
                "            \"descriptionformatted\": \"List of VAST Verification vendor attribute values that the player / client supports.\",\n" +
                "            \"example\": \"<code>moat.com-omid,ias.com-omid,doubleverify.com-omid</code>\"\n" +
                "        },\n" +
                "        {\n" +
                "            \"id\": 32,\n" +
                "            \"name\": \"OMIDPARTNER\",\n" +
                "            \"categoryname\": \"Capabilities Info\",\n" +
                "            \"categoryindex\": 6,\n" +
                "            \"datatype\": \"string\",\n" +
                "            \"type\": \"string\",\n" +
                "            \"introversion\": \"4.1\",\n" +
                "            \"support\": \"Required (if OM is supported)\",\n" +
                "            \"contexts\": \"All tracking pixels,VAST request URIs\",\n" +
                "            \"format\": \"{partner name}/{partner version}\",\n" +
                "            \"description\": \"An identifier of the OM SDK integration. This is the same as the “name” and “versionString” parameters of the OMID Partner object. This will allow creative providers to determine if the OM partner integration is acceptable.\\nNote - This value is essential for communicating the certification status of the OMID integration. If partner ID is not shared, verification vendors will not be able to properly measure and report on this inventory.\\n\\nIf partner name is not available, use “unknown”.\",\n" +
                "            \"descriptionformatted\": \"An identifier of the OM SDK integration. This is the same as the “name” and “versionString” parameters of the OMID Partner object. This will allow creative providers to determine if the OM partner integration is acceptable.<p>Note - This value is essential for communicating the certification status of the OMID integration. If partner ID is not shared, verification vendors will not be able to properly measure and report on this inventory.</p><p>If partner name is not available, use “unknown”.</p>\",\n" +
                "            \"example\": \"Unencoded: <code>MyIntegrationPartner/7.1</code><br>Encoded: <code>MyIntegrationPartner%2F7.1</code>\"\n" +
                "        },\n" +
                "        {\n" +
                "            \"id\": 33,\n" +
                "            \"name\": \"MEDIAMIME\",\n" +
                "            \"categoryname\": \"Capabilities Info\",\n" +
                "            \"categoryindex\": 6,\n" +
                "            \"datatype\": \"Array<string>\",\n" +
                "            \"type\": \"Array<string>\",\n" +
                "            \"introversion\": \"4.1\",\n" +
                "            \"support\": \"Optional\",\n" +
                "            \"contexts\": \"VAST request URIs\",\n" +
                "            \"format\": \"{type}/{subtype}\",\n" +
                "            \"description\": \"List of media MIME types supported by the player.\",\n" +
                "            \"descriptionformatted\": \"List of media MIME types supported by the player.\",\n" +
                "            \"example\": \"Unencoded: <code>video/mp4,application/x-mpegURL</code><br>Encoded: <code>video%2Fmp4,application%2Fx-mpegURL</code>\"\n" +
                "        },\n" +
                "        {\n" +
                "            \"id\": 34,\n" +
                "            \"name\": \"PLAYERCAPABILITIES\",\n" +
                "            \"categoryname\": \"Capabilities Info\",\n" +
                "            \"categoryindex\": 6,\n" +
                "            \"datatype\": \"Array<string>\",\n" +
                "            \"type\": \"Array<string>\",\n" +
                "            \"introversion\": \"4.1\",\n" +
                "            \"support\": \"Optional\",\n" +
                "            \"contexts\": \"All tracking pixels,VAST request URIs\",\n" +
                "            \"values\": \"see:PLAYERCAPABILITIES_values_worksheet\",\n" +
                "            \"description\": \"List of values that describe the player capabilities.\",\n" +
                "            \"descriptionformatted\": \"List of values that describe the player capabilities.\"\n" +
                "        },\n" +
                "        {\n" +
                "            \"id\": 35,\n" +
                "            \"name\": \"CLICKTYPE\",\n" +
                "            \"categoryname\": \"Capabilities Info\",\n" +
                "            \"categoryindex\": 6,\n" +
                "            \"datatype\": \"integer\",\n" +
                "            \"type\": \"integer\",\n" +
                "            \"introversion\": \"4.1\",\n" +
                "            \"support\": \"Optional\",\n" +
                "            \"contexts\": \"All tracking pixels,VAST request URIs\",\n" +
                "            \"values\": \"see:PLAYERSTATE_values_worksheet\",\n" +
                "            \"description\": \"Indicates the type of clickthrough supported by the player.\\n3 supersedes 2 in the case that there are both a link and a confirmation dialog.\",\n" +
                "            \"descriptionformatted\": \"Indicates the type of clickthrough supported by the player.\\n<p><code>3</code> supersedes <code>2</code> in the case that there are both a link and a confirmation dialog.</p>\",\n" +
                "            \"example\": \"<code>2</code>\"\n" +
                "        },\n" +
                "        {\n" +
                "            \"id\": 36,\n" +
                "            \"name\": \"PLAYERSTATE\",\n" +
                "            \"categoryname\": \"Player State Info\",\n" +
                "            \"categoryindex\": 7,\n" +
                "            \"datatype\": \"Array<string>\",\n" +
                "            \"type\": \"Array<string>\",\n" +
                "            \"introversion\": \"4.1\",\n" +
                "            \"support\": \"Optional\",\n" +
                "            \"contexts\": \"All tracking pixels\",\n" +
                "            \"values\": \"see:CLICKTYPE_values_worksheet\",\n" +
                "            \"description\": \"List of options indicating the current player state.\",\n" +
                "            \"descriptionformatted\": \"List of options indicating the current player state.\",\n" +
                "            \"example\": \"<code>muted,fullscreen</code>\"\n" +
                "        },\n" +
                "        {\n" +
                "            \"id\": 37,\n" +
                "            \"name\": \"INVENTORYSTATE\",\n" +
                "            \"categoryname\": \"Player State Info\",\n" +
                "            \"categoryindex\": 7,\n" +
                "            \"datatype\": \"Array<string>\",\n" +
                "            \"type\": \"Array<string>\",\n" +
                "            \"introversion\": \"4.1\",\n" +
                "            \"support\": \"Optional\",\n" +
                "            \"contexts\": \"All tracking pixels,VAST request URIs\",\n" +
                "            \"values\": \"see:INVENTORYSTATE_values_worksheet\",\n" +
                "            \"description\": \"List of options indicating attributes of the inventory.\",\n" +
                "            \"descriptionformatted\": \"List of options indicating attributes of the inventory.\",\n" +
                "            \"example\": \"<code>autoplayed,fullscreen</code>\"\n" +
                "        },\n" +
                "        {\n" +
                "            \"id\": 38,\n" +
                "            \"name\": \"PLAYERSIZE\",\n" +
                "            \"categoryname\": \"Player State Info\",\n" +
                "            \"categoryindex\": 7,\n" +
                "            \"datatype\": \"Array<integer>\",\n" +
                "            \"type\": \"Array<integer>\",\n" +
                "            \"introversion\": \"4.1\",\n" +
                "            \"support\": \"Optional\",\n" +
                "            \"contexts\": \"All tracking pixels,VAST request URIs\",\n" +
                "            \"description\": \"Integer width and height of the player, separated by a <code>coma</code>, measured in pixels (device-independent).\",\n" +
                "            \"descriptionformatted\": \"Integer width and height of the player, separated by a <code>coma</code>, measured in pixels (device-independent).\",\n" +
                "            \"example\": \"<code>640,360</code>\"\n" +
                "        },\n" +
                "        {\n" +
                "            \"id\": 39,\n" +
                "            \"name\": \"ADPLAYHEAD\",\n" +
                "            \"categoryname\": \"Player State Info\",\n" +
                "            \"categoryindex\": 7,\n" +
                "            \"datatype\": \"string\",\n" +
                "            \"type\": \"timecode\",\n" +
                "            \"introversion\": \"4.1\",\n" +
                "            \"support\": \"Optional\",\n" +
                "            \"contexts\": \"All tracking pixels\",\n" +
                "            \"format\": \"{HH:MM:SS.mmm}\",\n" +
                "            \"description\": \"Media playhead position.\",\n" +
                "            \"descriptionformatted\": \"Media playhead position.\",\n" +
                "            \"example\": \" Unencoded: <code>00:00:11.355</code><br>Encoded: <code>00%3A00%3A11.355</code>\"\n" +
                "        },\n" +
                "        {\n" +
                "            \"id\": 40,\n" +
                "            \"name\": \"ASSETURI\",\n" +
                "            \"categoryname\": \"Player State Info\",\n" +
                "            \"categoryindex\": 7,\n" +
                "            \"datatype\": \"string\",\n" +
                "            \"type\": \"string\",\n" +
                "            \"introversion\": \"3.0\",\n" +
                "            \"support\": \"Optional\",\n" +
                "            \"contexts\": \"All tracking pixels\",\n" +
                "            \"description\": \"The URI of the ad asset currently being played.\",\n" +
                "            \"descriptionformatted\": \"The URI of the ad asset currently being played.\",\n" +
                "            \"example\": \"Unencoded: <code>https://myadserver.com/video.mp4</code><br>Encoded: <code>https%3A%2F%2Fmyadserver.com%2Fvideo.mp4</code>\"\n" +
                "        },\n" +
                "        {\n" +
                "            \"id\": 41,\n" +
                "            \"name\": \"CONTENTID\",\n" +
                "            \"categoryname\": \"Player State Info\",\n" +
                "            \"categoryindex\": 7,\n" +
                "            \"datatype\": \"string\",\n" +
                "            \"type\": \"string\",\n" +
                "            \"introversion\": \"4.1\",\n" +
                "            \"support\": \"Optional\",\n" +
                "            \"contexts\": \"All tracking pixels,VAST request URIs\",\n" +
                "            \"format\": \"{registry_id}{space}{id_value}\",\n" +
                "            \"description\": \"The publisher-specific content identifier for the content asset into which the ad is being loaded or inserted. Only applicable to in-stream ads.\\nThe format of this field is similar to the [UNIVERSALADID] macro, consisting of a registry identifier and a registry-specific content identifier. If you are not using a public registry, you can use your own domain name as the registry identifier.\\n\\nIf this macro is provided, the provided content identifier should uniquely define a specific content asset.\",\n" +
                "            \"descriptionformatted\": \"The publisher-specific content identifier for the content asset into which the ad is being loaded or inserted. Only applicable to in-stream ads.<p>The format of this field is similar to the <code>[UNIVERSALADID]</code> macro, consisting of a registry identifier and a registry-specific content identifier. If you are not using a public registry, you can use your own domain name as the registry identifier.</p><p>If this macro is provided, the provided content identifier should uniquely define a specific content asset.</p>\",\n" +
                "            \"example\": \" Unencoded: <code>my-domain.com my-video-123</code><br>Encoded: <code>my-domain.com%20my-video-123</code>\"\n" +
                "        },\n" +
                "        {\n" +
                "            \"id\": 42,\n" +
                "            \"name\": \"CONTENTURI\",\n" +
                "            \"categoryname\": \"Player State Info\",\n" +
                "            \"categoryindex\": 7,\n" +
                "            \"datatype\": \"string\",\n" +
                "            \"type\": \"string\",\n" +
                "            \"introversion\": \"4.1\",\n" +
                "            \"support\": \"Optional\",\n" +
                "            \"contexts\": \"All tracking pixels,VAST request URIs\",\n" +
                "            \"description\": \"The URI of the main media content asset into which the ad is being loaded or inserted. Only applicable to in-stream ads.\",\n" +
                "            \"descriptionformatted\": \"The URI of the main media content asset into which the ad is being loaded or inserted. Only applicable to in-stream ads.\",\n" +
                "            \"example\": \"Unencoded: <code>https://mycontentserver.com/video.mp4</code><br>Encoded: <code>https%3A%2F%2Fmycontentserver.com%2Fvideo.mp4</code>\"\n" +
                "        },\n" +
                "        {\n" +
                "            \"id\": 43,\n" +
                "            \"name\": \"PODSEQUENCE\",\n" +
                "            \"categoryname\": \"Player State Info\",\n" +
                "            \"categoryindex\": 7,\n" +
                "            \"datatype\": \"integer\",\n" +
                "            \"type\": \"integer\",\n" +
                "            \"introversion\": \"4.1\",\n" +
                "            \"support\": \"Optional\",\n" +
                "            \"contexts\": \"All tracking pixels\",\n" +
                "            \"description\": \"The value of the sequence attribute on the <Ad> that is currently playing, if one is provided.\",\n" +
                "            \"descriptionformatted\": \"The value of the sequence attribute on the <code><Ad></code> that is currently playing, if one is provided.\",\n" +
                "            \"example\": \"<code>1</code>\"\n" +
                "        },\n" +
                "        {\n" +
                "            \"id\": 44,\n" +
                "            \"name\": \"ADSERVINGID\",\n" +
                "            \"categoryname\": \"Player State Info\",\n" +
                "            \"categoryindex\": 7,\n" +
                "            \"datatype\": \"string\",\n" +
                "            \"type\": \"string\",\n" +
                "            \"introversion\": \"4.1\",\n" +
                "            \"support\": \"Optional\",\n" +
                "            \"contexts\": \"All tracking pixels\",\n" +
                "            \"format\": \"{AD SERVER NAME}-{UUID}\",\n" +
                "            \"description\": \"The value of the <AdServingId> for the currently playing ad, as passed from the ad server.\",\n" +
                "            \"descriptionformatted\": \"The value of the <code><AdServingId></code> for the currently playing ad, as passed from the ad server.\",\n" +
                "            \"example\": \"<code>ServerName-47ed3bac-1768-4b9a-9d0e-0b92422ab066</code>\"\n" +
                "        },\n" +
                "        {\n" +
                "            \"id\": 45,\n" +
                "            \"name\": \"CLICKPOS\",\n" +
                "            \"categoryname\": \"Click Info\",\n" +
                "            \"categoryindex\": 8,\n" +
                "            \"datatype\": \"Array<number>(2)\",\n" +
                "            \"type\": \"Array<number>(2)\",\n" +
                "            \"introversion\": \"4.1\",\n" +
                "            \"support\": \"Optional\",\n" +
                "            \"contexts\": \"<code>ClickTracking</code> tracking pixels\",\n" +
                "            \"description\": \"Coordinates of the click relative to the area defined by the [PLAYERSIZE] macro, measured in css (device-independent) pixels.\",\n" +
                "            \"descriptionformatted\": \"Coordinates of the click relative to the area defined by the <code>[PLAYERSIZE]</code> macro, measured in css (device-independent) pixels. \",\n" +
                "            \"example\": \"<code>315,204</code>\"\n" +
                "        },\n" +
                "        {\n" +
                "            \"id\": 46,\n" +
                "            \"name\": \"ERRORCODE\",\n" +
                "            \"categoryname\": \"Error Info\",\n" +
                "            \"categoryindex\": 9,\n" +
                "            \"datatype\": \"integer\",\n" +
                "            \"type\": \"integer\",\n" +
                "            \"introversion\": \"3.0\",\n" +
                "            \"support\": \"Required\",\n" +
                "            \"contexts\": \"<code>Error</code> tracking pixels\",\n" +
                "            \"description\": \"VAST Error Code. Replaced with one of the error codes listed in section 2.3.6.3 when the associated error occurs; reserved for error tracking URIs.\",\n" +
                "            \"descriptionformatted\": \"VAST Error Code. Replaced with one of the error codes listed in section 2.3.6.3 when the associated error occurs; reserved for error tracking URIs.\",\n" +
                "            \"example\": \"<code>900</code>\"\n" +
                "        },\n" +
                "        {\n" +
                "            \"id\": 47,\n" +
                "            \"name\": \"REASON\",\n" +
                "            \"categoryname\": \"Verification Info\",\n" +
                "            \"categoryindex\": 10,\n" +
                "            \"datatype\": \"integer\",\n" +
                "            \"type\": \"integer\",\n" +
                "            \"introversion\": \"4.1\",\n" +
                "            \"support\": \"Required\",\n" +
                "            \"contexts\": \"<code>verificationNotExecuted </code> tracking pixels\",\n" +
                "            \"description\": \"Reason code for not executing verification.\",\n" +
                "            \"descriptionformatted\": \"Reason code for not executing verification.\",\n" +
                "            \"example\": \"<code>1</code>\"\n" +
                "        },\n" +
                "        {\n" +
                "            \"id\": 48,\n" +
                "            \"name\": \"LIMITADTRACKING\",\n" +
                "            \"categoryname\": \"Regulation Info\",\n" +
                "            \"categoryindex\": 11,\n" +
                "            \"datatype\": \"integer\",\n" +
                "            \"type\": \"integer\",\n" +
                "            \"introversion\": \"4.1\",\n" +
                "            \"support\": \"Required\",\n" +
                "            \"contexts\": \"All tracking pixels,VAST request URIs\",\n" +
                "            \"description\": \"The limit ad tracking setting of a device-specific advertising ID scheme. This value is a boolean, with “1” indicating that a user has opted for limited ad tracking, and “0” indicating that the user has not opted.\",\n" +
                "            \"descriptionformatted\": \"The limit ad tracking setting of a device-specific advertising ID scheme. This value is a boolean, with <code>1</code> indicating that a user has opted for limited ad tracking, and <code>0</code> indicating that the user has not opted.\",\n" +
                "            \"example\": \"<code>0</code>\"\n" +
                "        },\n" +
                "        {\n" +
                "            \"id\": 49,\n" +
                "            \"name\": \"REGULATIONS\",\n" +
                "            \"categoryname\": \"Regulation Info\",\n" +
                "            \"categoryindex\": 11,\n" +
                "            \"datatype\": \"Array<string>\",\n" +
                "            \"type\": \"Array<string>\",\n" +
                "            \"introversion\": \"4.1\",\n" +
                "            \"support\": \"Optional\",\n" +
                "            \"contexts\": \"All tracking pixels,VAST request URIs\",\n" +
                "            \"values\": \"see:REGULATIONS_values_worksheet\",\n" +
                "            \"description\": \"List of applicable regulations.\",\n" +
                "            \"descriptionformatted\": \"List of applicable regulations.\",\n" +
                "            \"example\": \"<code>gdpr</code>\"\n" +
                "        },\n" +
                "        {\n" +
                "            \"id\": 50,\n" +
                "            \"name\": \"GDPRCONSENT\",\n" +
                "            \"categoryname\": \"Regulation Info\",\n" +
                "            \"categoryindex\": 11,\n" +
                "            \"datatype\": \"string\",\n" +
                "            \"type\": \"string\",\n" +
                "            \"introversion\": \"4.1\",\n" +
                "            \"support\": \"Optional\",\n" +
                "            \"contexts\": \"All tracking pixels,VAST request URIs\",\n" +
                "            \"format\": \"Base-64 ecoded\",\n" +
                "            \"description\": \"Base64-encoded Cookie Value of IAB GDPR consent info.\",\n" +
                "            \"descriptionformatted\": \"Cookie Value of <a href=\\\"https://github.com/InteractiveAdvertisingBureau/GDPR-Transparency-and-Consent-Framework/blob/master/Draft_for_Public_Comment_Transparency%20&%20Consent%20Framework%20-%20cookie%20and%20vendor%20list%20format%20specification%20v1.0a.pdf\\\">IAB GDPR consent info</a>.\",\n" +
                "            \"example\": \"<code>BOLqFHuOLqFHuAABAENAAAAAAAAoAAA</code>\"\n" +
                "        },\n" +
                "        {\n" +
                "            \"id\": 51,\n" +
                "            \"name\": \"STOREID\",\n" +
                "            \"categoryname\": \"New Macros\",\n" +
                "            \"categoryindex\": 1,\n" +
                "            \"datatype\": \"string\",\n" +
                "            \"type\": \"string\",\n" +
                "            \"introversion\": \"4.x\",\n" +
                "            \"support\": \"Optional\",\n" +
                "            \"contexts\": \"All tracking pixels,VAST request URIs\",\n" +
                "            \"description\": \"For app ads, a platform-specific app store ID such as iTunes store ID. Could be the same as app bundle on some platforms.\",\n" +
                "            \"descriptionformatted\": \"For app ads, a platform-specific app store ID such as iTunes store ID. Could be the same as app bundle on some platforms.\",\n" +
                "            \"example\": \"<ul><li>iOS/tvOS - 886445756</li><li>Android - com.tubitv</li><li>Roku - 41468</li></ul>\"\n" +
                "        },\n" +
                "        {\n" +
                "            \"id\": 52,\n" +
                "            \"name\": \"STOREURL\",\n" +
                "            \"categoryname\": \"New Macros\",\n" +
                "            \"categoryindex\": 1,\n" +
                "            \"datatype\": \"string\",\n" +
                "            \"type\": \"string\",\n" +
                "            \"introversion\": \"4.x\",\n" +
                "            \"support\": \"Optional\",\n" +
                "            \"contexts\": \"All tracking pixels,VAST request URIs\",\n" +
                "            \"description\": \"For app ads, the app store URL for the installed app. This macro belongs in the 'Publisher Info' section longer term.\",\n" +
                "            \"descriptionformatted\": \"For app ads, the app store URL for the installed app.\",\n" +
                "            \"example\": \"<ul><li>iOS/tvOS - https://apps.apple.com/us/app/tubi-watch-movies-tv-shows/id886445756</li><li>Android - com.tubitv</li><li>Roku - 41468</li></ul>\"\n" +
                "        },\n" +
                "        {\n" +
                "            \"id\": 53,\n" +
                "            \"name\": \"PLAYBACKMETHODS\",\n" +
                "            \"categoryname\": \"New Macros\",\n" +
                "            \"categoryindex\": 1,\n" +
                "            \"datatype\": \"integer\",\n" +
                "            \"type\": \"integer\",\n" +
                "            \"introversion\": \"4.x\",\n" +
                "            \"support\": \"Optional\",\n" +
                "            \"contexts\": \"All tracking pixels,VAST request URIs\",\n" +
                "            \"values\": \"see:PLAYBACKMETHODS_values_worksheet\",\n" +
                "            \"description\": \"The value indicating attributes of the inventory such as auto-play and click-to-play activity.\",\n" +
                "            \"example\": \"<code>1</code>\"\n" +
                "        },\n" +
                "        {\n" +
                "            \"id\": 54,\n" +
                "            \"name\": \"CONTENTCAT\",\n" +
                "            \"categoryname\": \"New Macros\",\n" +
                "            \"categoryindex\": 1,\n" +
                "            \"datatype\": \"Array<string>\",\n" +
                "            \"type\": \"Array<string>\",\n" +
                "            \"introversion\": \"4.x\",\n" +
                "            \"support\": \"Optional\",\n" +
                "            \"contexts\": \"VAST request URIs\",\n" +
                "            \"format\": \"IDVALUE\",\n" +
                "            \"description\": \"List of IDs of the categories relevant to the content the ad is being requested for. \\nThe IDs are from the IABTL Content Taxonopmy 2.x.\",\n" +
                "            \"descriptionformatted\": \"List of IDs of the categories relevant to the content the ad is being requested for. \\nThe IDs are from the <a href=\\\"https://iabtechlab.com/standards/content-taxonomy/\\\"> IABTL Content Taxonopmy 2.x.</a>\",\n" +
                "            \"example\": \"“8”,”16”,”1001”,”1021”,”1026”,”1068”,”1215” \\nto reflect content that matches - \\nContent Categories: Automotive/Convertible (8), Auto Type/Performance Cars(16)\\nContent Channel: Editorial/Professional (1001)\\nContent Type: Review (1021)\\nContent Media Format: Mixed (1026)\\nContent Language: en (1068)\\nContent Source: Professionally Produced (1215)\"\n" +
                "        },\n" +
                "        {\n" +
                "            \"id\": 55,\n" +
                "            \"name\": \"GPPSTRING\",\n" +
                "            \"categoryname\": \"New Macros\",\n" +
                "            \"categoryindex\": 1,\n" +
                "            \"datatype\": \"string\",\n" +
                "            \"type\": \"string\",\n" +
                "            \"introversion\": \"4.x\",\n" +
                "            \"support\": \"Optional\",\n" +
                "            \"contexts\": \"All tracking pixels, VAST request URIs\",\n" +
                "            \"format\": \"Encoded\",\n" +
                "            \"description\": \"GPP String from the Global Privacy Platform.\",\n" +
                "            \"descriptionformatted\": \"GPP String from the <a href=\\\"https://github.com/InteractiveAdvertisingBureau/Global-Privacy-Platform/blob/main/Core/Consent%20String%20Specification.md\\\">Global Privacy Platform</a>.\",\n" +
                "            \"example\": \"<code>DBABMA~CPXxRfAPXxRfAAfKABENB-CgAAAAAAAAAAYgAAAAAAAA</code>\"\n" +
                "        },\n" +
                "        {\n" +
                "            \"id\": 56,\n" +
                "            \"name\": \"GPPSECTIONID\",\n" +
                "            \"categoryname\": \"New Macros\",\n" +
                "            \"categoryindex\": 1,\n" +
                "            \"datatype\": \"Array<integer>\",\n" +
                "            \"type\": \"Array<integer>\",\n" +
                "            \"introversion\": \"4.x\",\n" +
                "            \"support\": \"Optional\",\n" +
                "            \"contexts\": \"All tracking pixels, VAST request URIs\",\n" +
                "            \"values\": \"see:GPPSECTIONID_values_worksheet\",\n" +
                "            \"description\": \"Array of the Section ID(s) of the GPP string from GPPSTRING which should be applied. See the GPP Section Information for the list of Section IDs. GPP Section 3 (Header) and 4 (Signal Integrity) do not need to be included.\",\n" +
                "            \"descriptionformatted\": \"Array of the Section ID(s) of the GPP string from GPPSTRING which should be applied. See the <a href=\\\"https://github.com/InteractiveAdvertisingBureau/Global-Privacy-Platform/blob/main/Sections/Section%20Information.md\\\">GPP Section Information</a> for the list of Section IDs. GPP Section 3 (Header) and 4 (Signal Integrity) do not need to be included.\",\n" +
                "            \"example\": \"5,7\"\n" +
                "        },\n" +
                "        {\n" +
                "            \"id\": 57,\n" +
                "            \"name\": \"DSAREQUIRED\",\n" +
                "            \"categoryname\": \"New Macros\",\n" +
                "            \"categoryindex\": 1,\n" +
                "            \"datatype\": \"integer\",\n" +
                "            \"type\": \"integer\",\n" +
                "            \"introversion\": \"Next VAST release\",\n" +
                "            \"support\": \"Optional\",\n" +
                "            \"contexts\": \"VAST request URIs\",\n" +
                "            \"values\": \"see:DSAREQUIRED_values_worksheet\",\n" +
                "            \"description\": \"Flag to indicate if DSA information should be made available. This will signal if the bid request belongs to an Online Platform/VLOP, such that a buyer should respond with DSA Transparency information.\",\n" +
                "            \"descriptionformatted\": \"Flag to indicate if DSA information should be made available. This will signal if the bid request belongs to an Online Platform/VLOP, such that a buyer should respond with DSA Transparency information.\",\n" +
                "            \"example\": 1\n" +
                "        },\n" +
                "        {\n" +
                "            \"id\": 58,\n" +
                "            \"name\": \"DSAPARAMS\",\n" +
                "            \"categoryname\": \"New Macros\",\n" +
                "            \"categoryindex\": 1,\n" +
                "            \"datatype\": \"Array<integer>\",\n" +
                "            \"type\": \"Array<integer>\",\n" +
                "            \"introversion\": \"Next VAST release\",\n" +
                "            \"support\": \"Optional\",\n" +
                "            \"contexts\": \"VAST request URIs\",\n" +
                "            \"values\": \"see:DSAPARAMS_values_worksheet\",\n" +
                "            \"description\": \"Array of user parameters applied by the platform or sell-side. See the List and Definitions of User Parameters for a list of possible user parameters.  \",\n" +
                "            \"descriptionformatted\": \"Array of user parameters applied by the platform or sell-side. See the <a href=\\\"https://iabeurope.eu/wp-content/uploads/IAB-Europe-DSA-Transparency-Implementation-Guidelines-FINAL.pdf\\\">List and Definitions of User Parameters</a> for a list of possible user parameters.  \",\n" +
                "            \"example\": \"1_2\"\n" +
                "        },\n" +
                "        {\n" +
                "            \"id\": 59,\n" +
                "            \"name\": \"DSAPUBRENDER\",\n" +
                "            \"categoryname\": \"New Macros\",\n" +
                "            \"categoryindex\": 1,\n" +
                "            \"datatype\": \"integer\",\n" +
                "            \"type\": \"integer\",\n" +
                "            \"introversion\": \"Next VAST release\",\n" +
                "            \"support\": \"Optional\",\n" +
                "            \"contexts\": \"VAST request URIs\",\n" +
                "            \"values\": \"see:DSAPUBRENDER_values_worksheet\",\n" +
                "            \"description\": \"Signals if the publisher is able to and intends to render an icon or other appropriate user-facing symbol and display the DSA transparency info to the end user.\",\n" +
                "            \"descriptionformatted\": \"Signals if the publisher is able to and intends to render an icon or other appropriate user-facing symbol and display the DSA transparency info to the end user.\",\n" +
                "            \"example\": 1\n" +
                "        }\n" +
                "    ],\n" +
                "     \"DSAPUBRENDER_values\": [\n" +
                "        {\n" +
                "            \"value\": 1,\n" +
                "            \"description\": \"Publisher can't render\"\n" +
                "        },\n" +
                "        {\n" +
                "            \"value\": 2,\n" +
                "            \"description\": \"Publisher could render depending on adrender\"\n" +
                "        },\n" +
                "        {\n" +
                "            \"value\": 3,\n" +
                "            \"description\": \"Publisher will render\"\n" +
                "        }\n" +
                "    ],\n" +
                "    \"DSAPARAMS_values\": [\n" +
                "        {\n" +
                "            \"value\": 1,\n" +
                "            \"description\": \"Profiling\"\n" +
                "        },\n" +
                "        {\n" +
                "            \"value\": 2,\n" +
                "            \"description\": \"Basic advertising\"\n" +
                "        },\n" +
                "        {\n" +
                "            \"value\": 3,\n" +
                "            \"description\": \"Precise geolocation\"\n" +
                "        }\n" +
                "    ],\n" +
                "    \"DSAREQUIRED_values\": [\n" +
                "        {\n" +
                "            \"value\": 0,\n" +
                "            \"description\": \"Not required\"\n" +
                "        },\n" +
                "        {\n" +
                "            \"value\": 1,\n" +
                "            \"description\": \"Supported, bid responses with or without DSA object will be accepted\"\n" +
                "        },\n" +
                "        {\n" +
                "            \"value\": 2,\n" +
                "            \"description\": \"Required, bid responses without DSA object will not be accepted\"\n" +
                "        },\n" +
                "        {\n" +
                "            \"value\": 3,\n" +
                "            \"description\": \"Required, bid responses without DSA object will not be accepted, Publisher is an Online Platform\"\n" +
                "        }\n" +
                "    ],\n" +
                "    \"GPPSECTIONID_values\": [\n" +
                "        {\n" +
                "            \"value\": \"1: EU TCF v1 section (deprecated)\"\n" +
                "        },\n" +
                "        {\n" +
                "            \"value\": \"2: EU TCF v2 section\"\n" +
                "        },\n" +
                "        {\n" +
                "            \"value\": \"3: GPP Header section\"\n" +
                "        },\n" +
                "        {\n" +
                "            \"value\": \"4: GPP signal integrity section Connecticut section\"\n" +
                "        },\n" +
                "        {\n" +
                "            \"value\": \"5: Canadian TCF section\"\n" +
                "        },\n" +
                "        {\n" +
                "            \"value\": \"6: USPrivacy String Unencoded Format section\"\n" +
                "        },\n" +
                "        {\n" +
                "            \"value\": \"7: US - national section\"\n" +
                "        },\n" +
                "        {\n" +
                "            \"value\": \"8: US - California section\"\n" +
                "        },\n" +
                "        {\n" +
                "            \"value\": \"9: US - Virginia section\"\n" +
                "        },\n" +
                "        {\n" +
                "            \"value\": \"10: US - Colorado section\"\n" +
                "        },\n" +
                "        {\n" +
                "            \"value\": \"11: US - Utah section\"\n" +
                "        },\n" +
                "        {\n" +
                "            \"value\": \"12: US - Connecticut section\"\n" +
                "        }\n" +
                "    ],\n" +
                "    \"REGULATIONS_values\": [\n" +
                "        {\n" +
                "            \"value\": \"coppa\"\n" +
                "        },\n" +
                "        {\n" +
                "            \"value\": \"gdrp\"\n" +
                "        }\n" +
                "    ],\n" +
                "    \"INVENTORYSTATE_values\": [\n" +
                "        {\n" +
                "            \"value\": \"skippable\",\n" +
                "            \"description\": \"to indicate the ad can be skipped\"\n" +
                "        },\n" +
                "        {\n" +
                "            \"value\": \"autoplayed\",\n" +
                "            \"description\": \"to indicate the ad is autoplayed with audio unmuted\"\n" +
                "        },\n" +
                "        {\n" +
                "            \"value\": \"mautoplayed\",\n" +
                "            \"description\": \"to indicate the ad is autoplayed with audio muted\"\n" +
                "        },\n" +
                "        {\n" +
                "            \"value\": \"optin\",\n" +
                "            \"description\": \"to indicate the user takes an explicit action to knowingly start playback of the ad\"\n" +
                "        }\n" +
                "    ],\n" +
                "    \"PLAYERSTATE_values\": [\n" +
                "        {\n" +
                "            \"value\": \"muted\",\n" +
                "            \"description\": \"to indicate the player is currently muted\"\n" +
                "        },\n" +
                "        {\n" +
                "            \"value\": \"fullscreen\",\n" +
                "            \"description\": \"to indicate the player is currently fullscreen\"\n" +
                "        }\n" +
                "    ],\n" +
                "    \"CLICKTYPE_values\": [\n" +
                "        {\n" +
                "            \"value\": 0,\n" +
                "            \"description\": \"not clickable\"\n" +
                "        },\n" +
                "        {\n" +
                "            \"value\": 1,\n" +
                "            \"description\": \"clickable on full area of video\"\n" +
                "        },\n" +
                "        {\n" +
                "            \"value\": 2,\n" +
                "            \"description\": \"clickable only on associated button or link\"\n" +
                "        },\n" +
                "        {\n" +
                "            \"value\": 3,\n" +
                "            \"description\": \"clickable with confirmation dialog\"\n" +
                "        }\n" +
                "    ],\n" +
                "    \"PLAYBACKMETHODS_values\": [\n" +
                "        {\n" +
                "            \"value\": 1,\n" +
                "            \"description\": \"Initiates on Page Load with Sound On\"\n" +
                "        },\n" +
                "        {\n" +
                "            \"value\": 2,\n" +
                "            \"description\": \"Initiates on Page Load with Sound Off by Default\"\n" +
                "        },\n" +
                "        {\n" +
                "            \"value\": 3,\n" +
                "            \"description\": \"Initiates on Click with Sound On\"\n" +
                "        },\n" +
                "        {\n" +
                "            \"value\": 4,\n" +
                "            \"description\": \"Initiates on Mouse-Over with Sound On\"\n" +
                "        },\n" +
                "        {\n" +
                "            \"value\": 5,\n" +
                "            \"description\": \"Initiates on Entering Viewport with Sound On\"\n" +
                "        },\n" +
                "        {\n" +
                "            \"value\": 6,\n" +
                "            \"description\": \"Initiates on Entering Viewport with Sound Off by Default\"\n" +
                "        },\n" +
                "        {\n" +
                "            \"value\": 7,\n" +
                "            \"description\": \"Indicates content is playing back-to-back without any user interaction\"\n" +
                "        }\n" +
                "    ],\n" +
                "    \"PLAYERCAPABILITIES_values\": [\n" +
                "        {\n" +
                "            \"value\": \"skip\",\n" +
                "            \"description\": \"to indicate the user's ability to skip the ad\"\n" +
                "        },\n" +
                "        {\n" +
                "            \"value\": \"mute\",\n" +
                "            \"description\": \"to indicate the user's ability to mute/unmute audio\"\n" +
                "        },\n" +
                "        {\n" +
                "            \"value\": \"autoplay\",\n" +
                "            \"description\": \"to indicate the player's ability to autoplay media with audio; also implies mautoplay\"\n" +
                "        },\n" +
                "        {\n" +
                "            \"value\": \"mautoplay\",\n" +
                "            \"description\": \"to indicate the player's ability to autoplay media when muted\"\n" +
                "        },\n" +
                "        {\n" +
                "            \"value\": \"fullscreen\",\n" +
                "            \"description\": \"to indicate the user's ability to enter fullscreen\"\n" +
                "        },\n" +
                "        {\n" +
                "            \"value\": \"icon\",\n" +
                "            \"description\": \"to indicate the player's ability to render NAI icons from VAST\"\n" +
                "        }\n" +
                "    ],\n" +
                "    \"BREAKPOSITION_values\": [\n" +
                "        {\n" +
                "            \"value\": 1,\n" +
                "            \"description\": \"for pre-roll\"\n" +
                "        },\n" +
                "        {\n" +
                "            \"value\": 2,\n" +
                "            \"description\": \"for mid-roll\"\n" +
                "        },\n" +
                "        {\n" +
                "            \"value\": 3,\n" +
                "            \"description\": \"for post-roll\"\n" +
                "        },\n" +
                "        {\n" +
                "            \"value\": 4,\n" +
                "            \"description\": \"for standalone\"\n" +
                "        },\n" +
                "        {\n" +
                "            \"value\": 0,\n" +
                "            \"description\": \"for none of the above/other\"\n" +
                "        }\n" +
                "    ],\n" +
                "    \"SERVERSIDE_values\": [\n" +
                "        {\n" +
                "            \"value\": 0,\n" +
                "            \"description\": \"Client fires request or tracking call without server intermediary\"\n" +
                "        },\n" +
                "        {\n" +
                "            \"value\": 1,\n" +
                "            \"description\": \"Server fires request or tracking call on behalf of a client. The client told the server to act on its behalf\"\n" +
                "        },\n" +
                "        {\n" +
                "            \"value\": 2,\n" +
                "            \"description\": \"Server fires request or tracking call on behalf of another server, unknown party, or based on its own decision, without an explicit signal from the client\"\n" +
                "        }\n" +
                "    ],\n" +
                "    \"displaycolumns\": [\n" +
                "        {\n" +
                "            \"column\": \"datatype\"\n" +
                "        },\n" +
                "        {\n" +
                "            \"column\": \"introversion\"\n" +
                "        },\n" +
                "        {\n" +
                "            \"column\": \"support\"\n" +
                "        },\n" +
                "        {\n" +
                "            \"column\": \"contexts\"\n" +
                "        },\n" +
                "        {\n" +
                "            \"column\": \"format\"\n" +
                "        },\n" +
                "        {\n" +
                "            \"column\": \"values\"\n" +
                "        },\n" +
                "        {\n" +
                "            \"column\": \"descriptionformatted\"\n" +
                "        },\n" +
                "        {\n" +
                "            \"column\": \"example\"\n" +
                "        }\n" +
                "    ],\n" +
                "    \"labels\": [\n" +
                "        {\n" +
                "            \"property\": \"name\",\n" +
                "            \"label\": \"Macro\"\n" +
                "        },\n" +
                "        {\n" +
                "            \"property\": \"datatype\",\n" +
                "            \"label\": \"Data Type\"\n" +
                "        },\n" +
                "        {\n" +
                "            \"property\": \"type\",\n" +
                "            \"label\": \"Type\"\n" +
                "        },\n" +
                "        {\n" +
                "            \"property\": \"introversion\",\n" +
                "            \"label\": \"Introduced In\"\n" +
                "        },\n" +
                "        {\n" +
                "            \"property\": \"support\",\n" +
                "            \"label\": \"Support\"\n" +
                "        },\n" +
                "        {\n" +
                "            \"property\": \"contexts\",\n" +
                "            \"label\": \"Contexts\"\n" +
                "        },\n" +
                "        {\n" +
                "            \"property\": \"format\",\n" +
                "            \"label\": \"Format\"\n" +
                "        },\n" +
                "        {\n" +
                "            \"property\": \"values\",\n" +
                "            \"label\": \"Possible Values\"\n" +
                "        },\n" +
                "        {\n" +
                "            \"property\": \"descriptionformatted\",\n" +
                "            \"label\": \"Description\"\n" +
                "        },\n" +
                "        {\n" +
                "            \"property\": \"example\",\n" +
                "            \"label\": \"Example\"\n" +
                "        }\n" +
                "    ],\n" +
                "    \"datatypes\": [\n" +
                "        {\n" +
                "            \"name\": \"string\"\n" +
                "        },\n" +
                "        {\n" +
                "            \"name\": \"integer\"\n" +
                "        },\n" +
                "        {\n" +
                "            \"name\": \"number\"\n" +
                "        },\n" +
                "        {\n" +
                "            \"name\": \"Array<string>\"\n" +
                "        },\n" +
                "        {\n" +
                "            \"name\": \"Array<string>(2)\"\n" +
                "        },\n" +
                "        {\n" +
                "            \"name\": \"Array<number>\"\n" +
                "        },\n" +
                "        {\n" +
                "            \"name\": \"Array<number>(2)\"\n" +
                "        },\n" +
                "        {\n" +
                "            \"name\": \"Array<integer>\"\n" +
                "        },\n" +
                "        {\n" +
                "            \"name\": \"Array<integer>(2)\"\n" +
                "        },\n" +
                "        {\n" +
                "            \"name\": \"Array<float>\"\n" +
                "        },\n" +
                "        {\n" +
                "            \"name\": \"Array<float>(2)\"\n" +
                "        }\n" +
                "    ],\n" +
                "    \"categories\": [\n" +
                "        {\n" +
                "            \"index\": 1,\n" +
                "            \"name\": \"New Macros\"\n" +
                "        },\n" +
                "        {\n" +
                "            \"index\": 2,\n" +
                "            \"name\": \"Generic Macros\"\n" +
                "        },\n" +
                "        {\n" +
                "            \"index\": 3,\n" +
                "            \"name\": \"Ad Break Info\"\n" +
                "        },\n" +
                "        {\n" +
                "            \"index\": 4,\n" +
                "            \"name\": \"Client Info\"\n" +
                "        },\n" +
                "        {\n" +
                "            \"index\": 5,\n" +
                "            \"name\": \"Publisher Info\"\n" +
                "        },\n" +
                "        {\n" +
                "            \"index\": 6,\n" +
                "            \"name\": \"Capabilities Info\"\n" +
                "        },\n" +
                "        {\n" +
                "            \"index\": 7,\n" +
                "            \"name\": \"Player State Info\"\n" +
                "        },\n" +
                "        {\n" +
                "            \"index\": 8,\n" +
                "            \"name\": \"Click Info\"\n" +
                "        },\n" +
                "        {\n" +
                "            \"index\": 9,\n" +
                "            \"name\": \"Error Info\"\n" +
                "        },\n" +
                "        {\n" +
                "            \"index\": 10,\n" +
                "            \"name\": \"Verification Info\"\n" +
                "        },\n" +
                "        {\n" +
                "            \"index\": 11,\n" +
                "            \"name\": \"Regulation Info\"\n" +
                "        }\n" +
                "    ]\n" +
                "} "));
        p.parse();
        System.out.println(p.objects.peek());
    }


}
