package x.test.parameters;

import x.reader.ReaderImplementation;
import x.test.MessagesReadMethod;
import x.writer.MessageWriterType;

public class LaunchParameters {

    public Writer writer = new Writer();
    public Reader reader = new Reader();

    @Override
    public String toString() {
        return "LaunchParameters{" +
               "writer=" + writer +
               ", reader=" + reader +
               '}';
    }

    public static class Writer {
        public int threadsNumber;
        public MessageWriterType type;
        public int campaignsPerThread;
        public int usersPerCampaign;
        public int maxBatchSize;

        public int getCampaignsNumber() {
            return threadsNumber * campaignsPerThread;
        }

        @Override
        public String toString() {
            return "Writer{" +
                   "threadsNumber=" + threadsNumber +
                   ", type=" + type +
                   ", campaignsPerThread=" + campaignsPerThread +
                   ", usersPerCampaign=" + usersPerCampaign +
                   ", maxBatchSize=" + maxBatchSize +
                   '}';
        }
    }

    public static class Reader {
        public int threadsNumber;
        public MessagesReadMethod method;
        public ReaderImplementation implementation;

        @Override
        public String toString() {
            return "Reader{" +
                   "threadsNumber=" + threadsNumber +
                   ", method=" + method +
                   ", implementation=" + implementation +
                   '}';
        }
    }
}
