import java.io.*;
import java.net.Socket;

/**
 * Automated test harness for the BBoard server.
 * Usage: First start the server:
 *   java BBoard 9999 200 100 20 10 red white green yellow
 * Then run:
 *   java BBoardTest 9999
 *
 * Tests all RFC-defined commands, error cases, and edge cases.
 */
public class BBoardTest {

    private static int passed = 0;
    private static int failed = 0;
    private static int testNum = 0;

    private BufferedReader in;
    private PrintWriter out;
    private Socket socket;

    public static void main(String[] args) throws Exception {
        int port = 4554;
        if (args.length > 0) port = Integer.parseInt(args[0]);

        System.out.println("========================================");
        System.out.println("  BBoard Automated Test Suite");
        System.out.println("  Server: localhost:" + port);
        System.out.println("========================================\n");

        BBoardTest tester = new BBoardTest();

        // --- Section 1: Connection & Greeting ---
        System.out.println("--- Section 1: Connection & Greeting ---");
        tester.connect("localhost", port);
        tester.testGreeting();

        // --- Section 2: Empty Board Behavior (RFC 11.1) ---
        System.out.println("\n--- Section 2: Empty Board Behavior ---");
        tester.testEmptyBoardGet();
        tester.testEmptyBoardGetPins();
        tester.testEmptyBoardShake();
        tester.testEmptyBoardClear();
        tester.testEmptyBoardPin();

        // --- Section 3: POST Command ---
        System.out.println("\n--- Section 3: POST Command ---");
        tester.testPostSuccess();
        tester.testPostAtOrigin();
        tester.testPostAtMaxBound();
        tester.testPostOutOfBounds();
        tester.testPostNegativeCoords();
        tester.testPostInvalidColor();
        tester.testPostCompleteOverlap();
        tester.testPostPartialOverlap();
        tester.testPostMalformed();
        tester.testPostEmptyMessage();
        tester.testPostLongMessage();

        // --- Section 4: GET Command ---
        System.out.println("\n--- Section 4: GET Command ---");
        tester.testGetAll();
        tester.testGetColorFilter();
        tester.testGetContainsFilter();
        tester.testGetRefersToFilter();
        tester.testGetCombinedFilters();
        tester.testGetNoMatch();
        tester.testGetContainsOutOfBounds();
        tester.testGetInvalidFormat();

        // --- Section 5: PIN Command ---
        System.out.println("\n--- Section 5: PIN Command ---");
        tester.testPinSuccess();
        tester.testPinIdempotent();
        tester.testPinNoNote();
        tester.testPinOutOfBounds();
        tester.testPinMalformed();
        tester.testPinMultipleNotes();

        // --- Section 6: UNPIN Command ---
        System.out.println("\n--- Section 6: UNPIN Command ---");
        tester.testUnpinSuccess();
        tester.testUnpinNotFound();
        tester.testUnpinOutOfBounds();

        // --- Section 7: GET PINS ---
        System.out.println("\n--- Section 7: GET PINS ---");
        tester.testGetPins();

        // --- Section 8: SHAKE Command ---
        System.out.println("\n--- Section 8: SHAKE Command ---");
        tester.testShake();

        // --- Section 9: CLEAR Command ---
        System.out.println("\n--- Section 9: CLEAR Command ---");
        tester.testClear();

        // --- Section 10: Pinned Status in GET ---
        System.out.println("\n--- Section 10: Pinned Status ---");
        tester.testPinnedStatus();

        // --- Section 11: Whitespace Handling (RFC 6.1) ---
        System.out.println("\n--- Section 11: Whitespace Handling ---");
        tester.testLeadingWhitespace();
        tester.testTrailingWhitespace();

        // --- Section 12: Invalid/Unknown Commands ---
        System.out.println("\n--- Section 12: Invalid Commands ---");
        tester.testUnknownCommand();
        tester.testEmptyCommand();
        tester.testExtraArgsShake();
        tester.testExtraArgsClear();

        // --- Section 13: DISCONNECT ---
        System.out.println("\n--- Section 13: DISCONNECT ---");
        tester.testDisconnect();

        // --- Section 14: Persistence Across Clients ---
        System.out.println("\n--- Section 14: Persistence Across Clients ---");
        tester.testPersistenceAcrossClients(port);

        // --- Section 15: Multiple Concurrent Clients ---
        System.out.println("\n--- Section 15: Multiple Concurrent Clients ---");
        tester.testConcurrentClients(port);

        // --- Summary ---
        System.out.println("\n========================================");
        System.out.println("  RESULTS: " + passed + " passed, " + failed + " failed, " + (passed + failed) + " total");
        System.out.println("========================================");

        if (failed > 0) System.exit(1);
    }

    // ---- Connection helpers ----

    private void connect(String host, int port) throws Exception {
        socket = new Socket(host, port);
        in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()), true);
    }

    private void disconnect() {
        try { out.println("DISCONNECT"); in.readLine(); } catch (Exception ignored) {}
        try { if (out != null) out.close(); } catch (Exception ignored) {}
        try { if (in != null) in.close(); } catch (Exception ignored) {}
        try { if (socket != null) socket.close(); } catch (Exception ignored) {}
    }

    private String send(String cmd) throws Exception {
        out.println(cmd);
        return in.readLine();
    }

    private String sendFull(String cmd) throws Exception {
        out.println(cmd);
        String first = in.readLine();
        if (first == null) return null;
        StringBuilder sb = new StringBuilder(first);
        try {
            String[] parts = first.trim().split("\\s+");
            if (parts.length == 2 && parts[0].equals("OK")) {
                int count = Integer.parseInt(parts[1]);
                for (int i = 0; i < count; i++) {
                    String line = in.readLine();
                    if (line == null) break;
                    sb.append("\n").append(line);
                }
            }
        } catch (NumberFormatException ignored) {}
        return sb.toString();
    }

    // ---- Assertion helpers ----

    private void check(String testName, boolean condition, String details) {
        testNum++;
        if (condition) {
            passed++;
            System.out.println("  [PASS] #" + testNum + " " + testName);
        } else {
            failed++;
            System.out.println("  [FAIL] #" + testNum + " " + testName + " -- " + details);
        }
    }

    private void checkResponse(String testName, String actual, String expected) {
        check(testName, expected.equals(actual), "expected \"" + expected + "\", got \"" + actual + "\"");
    }

    private void checkStartsWith(String testName, String actual, String prefix) {
        check(testName, actual != null && actual.startsWith(prefix),
              "expected to start with \"" + prefix + "\", got \"" + actual + "\"");
    }

    // ---- Test cases ----

    private void testGreeting() throws Exception {
        String greeting = in.readLine();
        // Expected format: "200 100 20 10 red white green yellow"
        check("Greeting received", greeting != null && !greeting.isEmpty(), "greeting was null/empty");
        String[] parts = greeting.trim().split("\\s+");
        check("Greeting has >= 5 parts (dims + colors)", parts.length >= 5,
              "got " + parts.length + " parts: " + greeting);
        check("Greeting boardW=200", parts[0].equals("200"), "boardW=" + parts[0]);
        check("Greeting boardH=100", parts[1].equals("100"), "boardH=" + parts[1]);
        check("Greeting noteW=20",  parts[2].equals("20"),  "noteW=" + parts[2]);
        check("Greeting noteH=10",  parts[3].equals("10"),  "noteH=" + parts[3]);
    }

    // --- Section 2: Empty Board ---

    private void testEmptyBoardGet() throws Exception {
        String resp = sendFull("GET");
        checkResponse("GET on empty board", resp, "OK 0");
    }

    private void testEmptyBoardGetPins() throws Exception {
        String resp = sendFull("GET PINS");
        checkResponse("GET PINS on empty board", resp, "OK 0");
    }

    private void testEmptyBoardShake() throws Exception {
        String resp = send("SHAKE");
        checkResponse("SHAKE on empty board", resp, "OK SHAKE_COMPLETE");
    }

    private void testEmptyBoardClear() throws Exception {
        String resp = send("CLEAR");
        checkResponse("CLEAR on empty board", resp, "OK CLEAR_COMPLETE");
    }

    private void testEmptyBoardPin() throws Exception {
        String resp = send("PIN 10 10");
        checkStartsWith("PIN on empty board -> NO_NOTE", resp, "ERROR NO_NOTE_AT_COORDINATE");
    }

    // --- Section 3: POST ---

    private void testPostSuccess() throws Exception {
        String resp = send("POST 30 20 white Team meeting at 3PM");
        checkResponse("POST valid note", resp, "OK NOTE_POSTED");
    }

    private void testPostAtOrigin() throws Exception {
        String resp = send("POST 0 0 red Origin note");
        checkResponse("POST at (0,0)", resp, "OK NOTE_POSTED");
    }

    private void testPostAtMaxBound() throws Exception {
        // Board 200x100, note 20x10. Max valid: x=180, y=90
        String resp = send("POST 180 90 green Edge note");
        checkResponse("POST at max valid (180,90)", resp, "OK NOTE_POSTED");
    }

    private void testPostOutOfBounds() throws Exception {
        // x=181 => note extends to 200, which is out of [0,199]
        String resp = send("POST 181 90 white OOB right");
        checkStartsWith("POST out of bounds (right)", resp, "ERROR OUT_OF_BOUNDS");

        resp = send("POST 0 91 white OOB bottom");
        checkStartsWith("POST out of bounds (bottom)", resp, "ERROR OUT_OF_BOUNDS");
    }

    private void testPostNegativeCoords() throws Exception {
        String resp = send("POST -5 10 yellow Negative X");
        checkStartsWith("POST negative x", resp, "ERROR");
        // Could be OUT_OF_BOUNDS or INVALID_FORMAT depending on parsing
    }

    private void testPostInvalidColor() throws Exception {
        String resp = send("POST 15 10 purple High Hanging Fruit");
        checkStartsWith("POST invalid color", resp, "ERROR COLOR_NOT_SUPPORTED");
    }

    private void testPostCompleteOverlap() throws Exception {
        // Post at same coords as existing note (30,20 was posted earlier)
        String resp = send("POST 30 20 red Duplicate position");
        checkStartsWith("POST complete overlap", resp, "ERROR COMPLETE_OVERLAP");
    }

    private void testPostPartialOverlap() throws Exception {
        // Overlaps with (30,20) but different upper-left
        String resp = send("POST 35 25 white Partial overlap");
        checkResponse("POST partial overlap (allowed)", resp, "OK NOTE_POSTED");
    }

    private void testPostMalformed() throws Exception {
        String resp = send("POST");
        checkStartsWith("POST no args", resp, "ERROR INVALID_FORMAT");

        resp = send("POST abc def white bad");
        checkStartsWith("POST non-integer coords", resp, "ERROR INVALID_FORMAT");

        resp = send("POST 10 20 white");
        checkStartsWith("POST missing message", resp, "ERROR INVALID_FORMAT");
    }

    private void testPostEmptyMessage() throws Exception {
        String resp = send("POST 50 50 white ");
        checkStartsWith("POST empty message", resp, "ERROR INVALID_FORMAT");
    }

    private void testPostLongMessage() throws Exception {
        StringBuilder sb = new StringBuilder("POST 60 60 white ");
        for (int i = 0; i < 260; i++) sb.append("A");
        String resp = send(sb.toString());
        checkStartsWith("POST message > 256 chars", resp, "ERROR INVALID_FORMAT");
    }

    // --- Section 4: GET ---

    private void testGetAll() throws Exception {
        String resp = sendFull("GET");
        String[] lines = resp.split("\n");
        // We should have at least a few notes from previous POST tests
        check("GET all returns OK <count>", lines[0].startsWith("OK "), "got: " + lines[0]);
        int count = Integer.parseInt(lines[0].split("\\s+")[1]);
        check("GET all count matches data lines", lines.length - 1 == count,
              "header says " + count + " but got " + (lines.length - 1) + " data lines");
        if (lines.length > 1) {
            check("GET data lines start with NOTE", lines[1].startsWith("NOTE "), "got: " + lines[1]);
            check("GET data lines contain PINNED=", lines[1].contains("PINNED="), "got: " + lines[1]);
        }
    }

    private void testGetColorFilter() throws Exception {
        String resp = sendFull("GET color=white");
        String[] lines = resp.split("\n");
        check("GET color=white returns OK", lines[0].startsWith("OK "), "got: " + lines[0]);
        boolean allWhite = true;
        for (int i = 1; i < lines.length; i++) {
            if (!lines[i].contains(" white ")) { allWhite = false; break; }
        }
        check("GET color=white all notes are white", allWhite, "non-white notes in result");
    }

    private void testGetContainsFilter() throws Exception {
        // Point (32, 25) should be inside note at (30,20) with size 20x10
        String resp = sendFull("GET contains=32 25");
        String[] lines = resp.split("\n");
        check("GET contains=32 25 returns OK", lines[0].startsWith("OK "), "got: " + lines[0]);
        int count = Integer.parseInt(lines[0].split("\\s+")[1]);
        check("GET contains=32 25 finds note(s)", count >= 1, "count=" + count);
    }

    private void testGetRefersToFilter() throws Exception {
        String resp = sendFull("GET refersTo=Team meeting");
        String[] lines = resp.split("\n");
        check("GET refersTo returns OK", lines[0].startsWith("OK "), "got: " + lines[0]);
        int count = Integer.parseInt(lines[0].split("\\s+")[1]);
        check("GET refersTo=Team meeting finds note", count >= 1, "count=" + count);
    }

    private void testGetCombinedFilters() throws Exception {
        String resp = sendFull("GET color=white contains=32 25");
        String[] lines = resp.split("\n");
        check("GET combined filters returns OK", lines[0].startsWith("OK "), "got: " + lines[0]);
    }

    private void testGetNoMatch() throws Exception {
        String resp = sendFull("GET refersTo=ZZZZNONEXISTENT");
        checkResponse("GET no match returns OK 0", resp, "OK 0");
    }

    private void testGetContainsOutOfBounds() throws Exception {
        String resp = sendFull("GET contains=999 999");
        checkStartsWith("GET contains OOB", resp, "ERROR OUT_OF_BOUNDS");
    }

    private void testGetInvalidFormat() throws Exception {
        String resp = sendFull("GET color");
        checkStartsWith("GET invalid format (color without =)", resp, "ERROR INVALID_FORMAT");

        resp = sendFull("GET contains=abc def");
        checkStartsWith("GET contains non-integer", resp, "ERROR INVALID_FORMAT");
    }

    // --- Section 5: PIN ---

    private void testPinSuccess() throws Exception {
        // Pin at (35, 25) which is inside note at (30,20)
        String resp = send("PIN 35 25");
        checkResponse("PIN success", resp, "OK PIN_ADDED");
    }

    private void testPinIdempotent() throws Exception {
        // Same pin again
        String resp = send("PIN 35 25");
        checkResponse("PIN idempotent", resp, "OK PIN_ADDED");
    }

    private void testPinNoNote() throws Exception {
        String resp = send("PIN 199 0");
        checkStartsWith("PIN no note at coordinate", resp, "ERROR NO_NOTE_AT_COORDINATE");
    }

    private void testPinOutOfBounds() throws Exception {
        String resp = send("PIN 999 999");
        checkStartsWith("PIN out of bounds", resp, "ERROR OUT_OF_BOUNDS");

        resp = send("PIN -1 5");
        checkStartsWith("PIN negative coord", resp, "ERROR");
    }

    private void testPinMalformed() throws Exception {
        String resp = send("PIN 10");
        checkStartsWith("PIN missing y", resp, "ERROR INVALID_FORMAT");

        resp = send("PIN abc 10");
        checkStartsWith("PIN non-integer", resp, "ERROR INVALID_FORMAT");
    }

    private void testPinMultipleNotes() throws Exception {
        // Notes at (30,20) and (35,25) overlap in area [35,39]x[25,29]
        // Pin at (37, 27) should affect both
        String resp = send("PIN 37 27");
        checkResponse("PIN in overlap area", resp, "OK PIN_ADDED");
    }

    // --- Section 6: UNPIN ---

    private void testUnpinSuccess() throws Exception {
        String resp = send("UNPIN 37 27");
        checkResponse("UNPIN success", resp, "OK PIN_REMOVED");
    }

    private void testUnpinNotFound() throws Exception {
        String resp = send("UNPIN 37 27");
        checkStartsWith("UNPIN no pin there", resp, "ERROR PIN_NOT_FOUND");
    }

    private void testUnpinOutOfBounds() throws Exception {
        String resp = send("UNPIN 999 999");
        checkStartsWith("UNPIN out of bounds", resp, "ERROR OUT_OF_BOUNDS");
    }

    // --- Section 7: GET PINS ---

    private void testGetPins() throws Exception {
        String resp = sendFull("GET PINS");
        String[] lines = resp.split("\n");
        check("GET PINS returns OK <count>", lines[0].startsWith("OK "), "got: " + lines[0]);
        int count = Integer.parseInt(lines[0].split("\\s+")[1]);
        check("GET PINS count > 0 (pin at 35,25 exists)", count >= 1, "count=" + count);
        if (lines.length > 1) {
            check("GET PINS data lines start with PIN", lines[1].startsWith("PIN "), "got: " + lines[1]);
        }
    }

    // --- Section 8: SHAKE ---

    private void testShake() throws Exception {
        // Post an unpinned note, then shake
        send("POST 100 50 yellow Unpinned shaker test");
        // Verify it exists
        String beforeShake = sendFull("GET refersTo=Unpinned shaker test");
        int countBefore = Integer.parseInt(beforeShake.split("\n")[0].split("\\s+")[1]);
        check("SHAKE: unpinned note exists before shake", countBefore >= 1, "count=" + countBefore);

        String resp = send("SHAKE");
        checkResponse("SHAKE success", resp, "OK SHAKE_COMPLETE");

        // Verify unpinned note is gone
        String afterShake = sendFull("GET refersTo=Unpinned shaker test");
        int countAfter = Integer.parseInt(afterShake.split("\n")[0].split("\\s+")[1]);
        check("SHAKE: unpinned note removed", countAfter == 0, "count=" + countAfter);

        // Pinned notes should still exist (note at 30,20 has a pin at 35,25)
        String pinnedCheck = sendFull("GET refersTo=Team meeting");
        int pinnedCount = Integer.parseInt(pinnedCheck.split("\n")[0].split("\\s+")[1]);
        check("SHAKE: pinned note persists", pinnedCount >= 1, "count=" + pinnedCount);
    }

    // --- Section 9: CLEAR ---

    private void testClear() throws Exception {
        String resp = send("CLEAR");
        checkResponse("CLEAR success", resp, "OK CLEAR_COMPLETE");

        // Board should be empty
        String getResp = sendFull("GET");
        checkResponse("CLEAR: board empty after clear", getResp, "OK 0");

        String pinsResp = sendFull("GET PINS");
        checkResponse("CLEAR: no pins after clear", pinsResp, "OK 0");
    }

    // --- Section 10: Pinned Status ---

    private void testPinnedStatus() throws Exception {
        // Fresh board after CLEAR. Post a note, check PINNED=false
        send("POST 10 10 red Check pinned status");
        String resp = sendFull("GET");
        check("Note initially PINNED=false", resp.contains("PINNED=false"), "got: " + resp);

        // Pin the note
        send("PIN 15 15");
        resp = sendFull("GET");
        check("Note PINNED=true after PIN", resp.contains("PINNED=true"), "got: " + resp);

        // Unpin
        send("UNPIN 15 15");
        resp = sendFull("GET");
        check("Note PINNED=false after UNPIN last pin", resp.contains("PINNED=false"), "got: " + resp);

        // Add two pins, remove one - should still be pinned
        send("PIN 12 12");
        send("PIN 14 14");
        resp = sendFull("GET");
        check("Note PINNED=true with 2 pins", resp.contains("PINNED=true"), "got: " + resp);

        send("UNPIN 12 12");
        resp = sendFull("GET");
        check("Note still PINNED=true with 1 pin remaining", resp.contains("PINNED=true"), "got: " + resp);

        send("CLEAR"); // cleanup
    }

    // --- Section 11: Whitespace ---

    private void testLeadingWhitespace() throws Exception {
        String resp = send("   CLEAR");
        checkResponse("Leading whitespace handled", resp, "OK CLEAR_COMPLETE");
    }

    private void testTrailingWhitespace() throws Exception {
        String resp = send("CLEAR   ");
        checkResponse("Trailing whitespace handled", resp, "OK CLEAR_COMPLETE");
    }

    // --- Section 12: Invalid Commands ---

    private void testUnknownCommand() throws Exception {
        String resp = send("FOOBAR");
        checkStartsWith("Unknown command", resp, "ERROR INVALID_FORMAT");
    }

    private void testEmptyCommand() throws Exception {
        String resp = send("");
        checkStartsWith("Empty command", resp, "ERROR INVALID_FORMAT");
    }

    private void testExtraArgsShake() throws Exception {
        String resp = send("SHAKE extra stuff");
        checkStartsWith("SHAKE with extra args", resp, "ERROR INVALID_FORMAT");
    }

    private void testExtraArgsClear() throws Exception {
        String resp = send("CLEAR extra stuff");
        checkStartsWith("CLEAR with extra args", resp, "ERROR INVALID_FORMAT");
    }

    // --- Section 13: DISCONNECT ---

    private void testDisconnect() throws Exception {
        String resp = send("DISCONNECT");
        checkResponse("DISCONNECT response", resp, "OK DISCONNECTING");
        // Connection should be closed by server after this
        socket.close();
    }

    // --- Section 14: Persistence Across Clients ---

    private void testPersistenceAcrossClients(int port) throws Exception {
        // Connect client 1, post a note, disconnect
        connect("localhost", port);
        in.readLine(); // greeting
        send("POST 10 10 green Persistent note");

        disconnect();

        // Connect client 2, verify note persists
        connect("localhost", port);
        in.readLine(); // greeting
        String resp = sendFull("GET refersTo=Persistent note");
        int count = Integer.parseInt(resp.split("\n")[0].split("\\s+")[1]);
        check("Note persists across client sessions", count >= 1, "count=" + count);

        send("CLEAR"); // cleanup
        disconnect();
    }

    // --- Section 15: Concurrent Clients ---

    private void testConcurrentClients(int port) throws Exception {
        final int NUM_CLIENTS = 5;
        final boolean[] success = new boolean[NUM_CLIENTS];
        Thread[] threads = new Thread[NUM_CLIENTS];

        // Clear board first
        connect("localhost", port);
        in.readLine();
        send("CLEAR");
        disconnect();

        for (int i = 0; i < NUM_CLIENTS; i++) {
            final int idx = i;
            threads[i] = new Thread(() -> {
                try {
                    Socket s = new Socket("localhost", port);
                    BufferedReader reader = new BufferedReader(new InputStreamReader(s.getInputStream()));
                    PrintWriter writer = new PrintWriter(new OutputStreamWriter(s.getOutputStream()), true);
                    reader.readLine(); // greeting

                    // Each client posts a note at a unique position
                    writer.println("POST " + (idx * 25) + " " + (idx * 15) + " red Client" + idx + "_note");
                    String resp = reader.readLine();
                    success[idx] = "OK NOTE_POSTED".equals(resp);

                    writer.println("DISCONNECT");
                    reader.readLine();
                    s.close();
                } catch (Exception e) {
                    success[idx] = false;
                }
            });
            threads[i].start();
        }

        for (Thread t : threads) t.join(5000);

        int succCount = 0;
        for (boolean s : success) if (s) succCount++;
        check("Concurrent clients: all " + NUM_CLIENTS + " POSTs succeed", succCount == NUM_CLIENTS,
              succCount + "/" + NUM_CLIENTS + " succeeded");

        // Verify all notes exist
        connect("localhost", port);
        in.readLine(); // greeting
        String resp = sendFull("GET");
        int noteCount = Integer.parseInt(resp.split("\n")[0].split("\\s+")[1]);
        check("Concurrent clients: all notes on board", noteCount == NUM_CLIENTS,
              "expected " + NUM_CLIENTS + ", got " + noteCount);

        send("CLEAR");
        disconnect();
    }
}
