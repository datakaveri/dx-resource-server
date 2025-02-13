package iudx.resource.server.database.elastic.util;

public interface ProgressListener {

  void updateProgress(double progress);

  void finish();
}
