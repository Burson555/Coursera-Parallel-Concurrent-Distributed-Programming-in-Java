package edu.coursera.distributed;

import java.net.ServerSocket;
import java.net.Socket;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.File;

/**
 * A basic and very limited implementation of a file server that responds to GET
 * requests from HTTP clients.
 */
public final class FileServer {
    /**
     * Main entrypoint for the basic file server.
     *
     * @param socket Provided socket to accept connections on.
     * @param fs A proxy filesystem to serve files from. See the PCDPFilesystem
     *           class for more detailed documentation of its usage.
     * @throws IOException If an I/O error is detected on the server. This
     *                     should be a fatal error, your file server
     *                     implementation is not expected to ever throw
     *                     IOExceptions during normal operation.
     */
    public void run(final ServerSocket socket, final PCDPFilesystem fs)
            throws IOException {
        /*
         * Enter a spin loop for handling client requests to the provided
         * ServerSocket object.
         */
        while (true) {

            // TODO 1) Use socket.accept to get a Socket object
            Socket s = socket.accept();

            /*
             * TODO 2) Using Socket.getInputStream(), parse the received HTTP
             * packet. In particular, we are interested in confirming this
             * message is a GET and parsing out the path to the file we are
             * GETing. Recall that for GET HTTP packets, the first line of the
             * received packet will look something like:
             *
             *     GET /path/to/file HTTP/1.1
             */
            InputStream input_stream = s.getInputStream();
            InputStreamReader input_stream_reader = new InputStreamReader(input_stream);
            BufferedReader buffered_reader = new BufferedReader(input_stream_reader);

            String msg = buffered_reader.readLine();
            assert msg != null;
            assert msg.startsWith("GET");
            final String path = msg.split(" ")[1];

            /*
             * TODO 3) Using the parsed path to the target file, construct an
             * HTTP reply and write it to Socket.getOutputStream(). If the file
             * exists, the HTTP reply should be formatted as follows:
             *
             *   HTTP/1.0 200 OK\r\n
             *   Server: FileServer\r\n
             *   \r\n
             *   FILE CONTENTS HERE\r\n
             *
             * If the specified file does not exist, you should return a reply
             * with an error code 404 Not Found. This reply should be formatted
             * as:
             *
             *   HTTP/1.0 404 Not Found\r\n
             *   Server: FileServer\r\n
             *   \r\n
             *
             * Don't forget to close the output stream.
             */
            OutputStream output_stream = s.getOutputStream();
            PrintWriter print_writer = new PrintWriter(output_stream);

            PCDPPath full_path = new PCDPPath(path);
            String contents = fs.readFile(full_path);

            if (contents == null) {
                print_writer.write("HTTP/1.0 404 Not Found\r\n");
                // print_writer.flush();
                print_writer.write("Server: FileServer\r\n");
                // print_writer.flush();
                print_writer.write("\r\n");
                // print_writer.flush();
            } else {
                print_writer.write("HTTP/1.0 200 OK\r\n");
                // print_writer.flush();
                print_writer.write("Server: FileServer\r\n");
                // print_writer.flush();
                print_writer.write("\r\n");
                // print_writer.flush();
                print_writer.write(contents);
                // print_writer.flush();
            }
            // print_writer.flush();
            print_writer.close();
        }
    }
}
