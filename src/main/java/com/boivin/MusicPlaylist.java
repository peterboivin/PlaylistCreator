package com.boivin;

import java.io.*;
import java.util.*;
import java.util.stream.Collectors;

public class MusicPlaylist {
    public static void main(String[] args) throws IOException {
        Properties prop = new Properties();
        InputStream input = MusicPlaylist.class.getResourceAsStream("/application.properties");
        prop.load(input);

        // Read in the "My Likes.csv" file and extract the song titles
        ArrayList<String> songTitles = new ArrayList<>();
        Set<File> songsFoundSet = new HashSet<>();

        try {
            BufferedReader br = new BufferedReader(new FileReader(prop.getProperty("my.likes.file")));
            String line;
            while ((line = br.readLine()) != null) {
                String[] parts = line.split(",");
                String songTitle = parts[0];
                songTitles.add(songTitle);
            }
            br.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Scan the hard drive for music files and save their locations
        ArrayList<File> musicFileLocations = new ArrayList<>();
        File root = new File(prop.getProperty("music.location")); // Change this to the root directory of your hard drive
        scanForMusicFiles(root, musicFileLocations);

        // Find the file locations of the songs in "My Likes.csv"
        ArrayList<File> songFileLocations = new ArrayList<>();
        for (String songTitle : songTitles) {
            File songFileLocation = findSongFileLocation(songTitle, musicFileLocations, songsFoundSet);
            if (songFileLocation != null) {
                songFileLocations.add(songFileLocation);
            }
        }

        Set<String> difference = new HashSet<>(songTitles);
        Set<String> songsFoundStringSet = songsFoundSet.stream()
                .map(File::getPath)
                .collect(Collectors.toSet());
        difference.removeAll(songsFoundStringSet);

        FileWriter differenceW = new FileWriter(prop.getProperty("difference.file.name"));
        difference.forEach(
                file -> {
                    try {
                        differenceW.write(String.valueOf(file));
                        differenceW.append("\n");
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
        );

        // Create the playlist file
        try {
            FileWriter fw = new FileWriter(prop.getProperty("playlist.file.name"));
            fw.write("#EXTM3U" + "\n");
            for (File songFileLocation : songFileLocations) {
                String fileName = songFileLocation.getName();
                fw.write("#EXTINF:0," + fileName + "\n");
                fw.write(songFileLocation + "\n");
            }
            fw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void scanForMusicFiles(File dir, ArrayList<File> musicFileLocations) {
        File[] files = dir.listFiles();

        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    scanForMusicFiles(file, musicFileLocations);
                } else {
                    String fileName = file.getName();
                    if (fileName.endsWith(".mp3") || fileName.endsWith(".m4a") || fileName.endsWith(".flac") && !file.isHidden()) {
                        musicFileLocations.add(file);
                    }
                }
            }
        }
    }

    private static File findSongFileLocation(String songTitle, ArrayList<File> musicFileLocations, Set<File> songsFoundSet) {
        for (File musicFileLocation : musicFileLocations) {
            String fileName = musicFileLocation.getName();
            if (fileName.contains(songTitle)) {
                songsFoundSet.add(new File(songTitle));
                return musicFileLocation;
            }
        }
        return null;
    }

    private static String getFileExtension(File fileName) {
        int dotIndex = fileName.getName().lastIndexOf(".");
        if (dotIndex == -1) {
            return "";
        } else {
            String fileEnding = fileName.getName().substring(dotIndex + 1);
            String fileEndingClean = new String();
            if (!fileEnding.equalsIgnoreCase("DS_Store") && !fileName.isHidden()) {
                fileEndingClean = fileEnding;
            }
            return fileEndingClean;
        }
    }
}
