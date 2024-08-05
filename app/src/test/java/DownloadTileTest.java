import uk.minersonline.open_infotainment.apps.navigation.TileDownloader;

import java.io.IOException;

public class DownloadTileTest {
	public static void main(String[] args) {
		TileDownloader downloader = new TileDownloader();
		int x = 0, y = 0, z = 1; // Example tile coordinates
		try {
			byte[] tileData = downloader.getTile(x, y, z);
			// Convert tileData to a texture and render
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
