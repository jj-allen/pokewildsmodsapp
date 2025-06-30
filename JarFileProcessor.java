import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.*;
import javax.imageio.ImageIO;
import java.util.regex.*;

//<!--02/09/25 - Made file bigger, more responsive, attempted filter for base_stats.asm-->
//<!--02/15/25 - SUCCESS! All directories now properly update. -->

//<!--02/15/25 - NEXT MILESTONE: CREATE GUI WITH JFRAME-->
//<!--02/19/25 - INVESTIGATED JFRAME, LAUNCHED 1ST MODEL WINDOW-->
//https://docs.oracle.com/javase/tutorial/displayCode.html?code=https://docs.oracle.com/javase/tutorial/uiswing/examples/components/FrameDemo2Project/src/components/FrameDemo2.java
public class JarFileProcessor implements Runnable{
    private String jarPath, modsPath, movesPath, statsPath;
    public List<Path> moveDirs;
    public List<Path> statDirs;
    public HashMap<Integer, String> entryMoves;
    protected HashMap<Integer,BufferedImage> images;
    protected String validMoves;
    protected static List<Path> targetPaths;
    protected HashMap<Integer,String> entries;
    //protected static HashMap<Integer,Path> entriesDir;

    public JarFileProcessor(String jarPath, String modsPath, String movesPath, String statsPath){
        this.jarPath = jarPath;
        this.modsPath = modsPath;
        this.movesPath = movesPath;
        this.statsPath = statsPath;
        this.moveDirs = new ArrayList<>();
        this.statDirs = new ArrayList<>();
        this.images = new HashMap<>();
        this.entries = new HashMap<>();
        this.entryMoves = new HashMap<>();
        this.validMoves = getValidMoves(jarPath);
        JarFileProcessor.targetPaths = new ArrayList<>();
    }

    @Override
    public void run(){
        try{
            List<Path> emptyDirs = new ArrayList<>();
            getTargetDirectories(jarPath, modsPath, emptyDirs, moveDirs, statDirs, 
                                movesPath, statsPath, images, entries, entryMoves);
            System.out.println("\nCurrent Empty Directories:" + emptyDirs);
        } catch (IOException e) { e.printStackTrace(); }
    }

    protected enum Type {
        NORMAL, FLYING, FIRE, WATER, GRASS, ELECTRIC, ICE, FIGHTING,
        POISON, GROUND, ROCK, BUG, GHOST, STEEL, PSYCHIC, DRAGON, DARK,
        FAIRY;
    }
    
    public List<BufferedImage> getImages(){
        return new ArrayList<BufferedImage>(images.values().stream().toList());
    }

    public String[] showData(HashMap<Integer, String> map, int index){
        String[] g = map.get(index).split(System.lineSeparator());
        return g;
    }

    public String[] getNames(){
        String[] f = new String[targetPaths.size()];
        int t = 0;
        for(Path r : targetPaths){
            f[t] = formatText(r.getFileName().toString());
            t++;
        }
        return f;
    }
    //only called if base_stats.asm is missing
    public static void appendEmptyStats(String jarPath, String statsPath, 
                                        Path target, HashMap<Integer,String> entries) 
                                        throws IOException {
            try (ZipFile zipFile = new ZipFile(jarPath)) {
            ZipEntry entry = zipFile.getEntry(statsPath + target.getFileName().toString() + ".asm");
            if (entry != null) {
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(zipFile.getInputStream(entry)))) {
                    String line;
                    StringBuilder content = new StringBuilder();
                    while ((line = reader.readLine()) != null) {
                        content.append(line).append(System.lineSeparator());
                    }
                    // Write to target directories
                    Files.writeString(target.resolve("base_stats.asm"), content.toString());
                    entries.merge(entries.keySet().size(), content.toString(), String::concat);
                    reader.close();
                } catch (IOException e) {
                    System.out.println(String.format("InputStream could not " +
                            "be created at %s: ", entry.getName()));
                    e.printStackTrace();
                } finally {
                    System.out.println(String.format("Base stats for %s successfully updated!",
                                        target.getFileName().toString()));
                }
            }
            zipFile.close();
        } catch (IOException e) {
            System.out.println(String.format("Could not find %s", statsPath + "!"));
            e.printStackTrace();
        } finally { System.out.println("Stat directory read completed."); }
    }
    //only called if evos_attacks.asm is missing
    public static void appendEmptyMoves(String jarPath, String movesPath, 
                                        Path target, HashMap<Integer,String> entryMoves) 
                                        throws IOException {
        // Process the JAR file
        try (ZipFile zipFile = new ZipFile(jarPath)) {
            ZipEntry entry = zipFile.getEntry(movesPath);
            if (entry != null) {
                try (BufferedReader reader = new BufferedReader(new 
                    InputStreamReader(zipFile.getInputStream(entry)))) {
                    String line;
                    boolean copyLines = false;
                    StringBuilder content = new StringBuilder();
                    String f = target.getFileName().toString();
                    while ((line = reader.readLine()) != null) {
                        if (line.toLowerCase().contains(f) && line.endsWith(":")) 
                        { copyLines = true;} 
                        else if (copyLines && line.trim().isEmpty()) { break; } 
                        else if (copyLines) { content.append(line).append(System.lineSeparator()); }
                    }
                    Files.writeString(target.resolve("evos_attacks.asm"), content.toString());
                    entryMoves.merge(entryMoves.keySet().size(), content.toString(), String::concat);
                    // Reset reader to beginning of the file for next directory processing
                    reader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    System.out.println(String.format("Archive %s successfully read!",
                            entry.getName()));
                }
            }
            zipFile.close();
        } finally { System.out.println("Move directory read completed."); }
    }

    //read modsPath file tree to Map, MAKE GLOBAL VAR INSTEAD - 3/28/25
    public static void updateEntries(Integer key, Path entry, HashMap<Integer,String> entries, 
                                    HashMap<Integer,String> entryMoves){
        String p = "";
        String[] target = {"base_stats.asm", "evos_attacks.asm"};
        try{
            p = Files.readString(entry);
            // Pattern to extract relevant parts, A - base_stats.asm, B - evos_attacks.asm
            //A - (db|dn\\s*)([\\w_ ,]+)
            Pattern patternA = Pattern.compile("(\\t(db|dn) )([\\w_, ]+)(; [\\w ]+)?");
            Pattern patternB = Pattern.compile("(\\tdb\\s*)([\\w_]+),\\s+([\\w_]+)([\\w,_ ]+)?");
            Matcher matcher;
            // 04/18/25 - MOAR DEBUGGING YAYYY :DDD
            try (BufferedReader reader = new BufferedReader(new StringReader(p))) {
                String line;
                StringBuilder sb = new StringBuilder();
                if (entry.getFileName().toString().equals(target[0])) {
                    matcher = patternA.matcher(p);
                    while ((line = reader.readLine()) != null) {
                        //filter out first line containing name; name must remain constant
                        if (!line.trim().isBlank() && matcher.find() && 
                            !line.contains(entry.getParent().getFileName().toString()))
                            sb.append(matcher.group(3).strip()).append(System.lineSeparator());
                    }
                    entries.put(key, sb.toString());
                } else if (entry.getFileName().toString().equals(target[1])) {
                    matcher = patternB.matcher(p);
                    while ((line = reader.readLine()) != null) {
                        if (!line.trim().isBlank() && matcher.find()) {
                            //p = formatText(matcher.group(2));
                            if (line.matches("\\t([\\D, ]+|[\\D, \\d]+)")
                                && matcher.group(4) != null)
                                { sb.append(matcher.group(2) + " " 
                                    + matcher.group(3)
                                    + matcher.group(4).replace(",",""))
                                    .append(System.lineSeparator()); }
                            else
                                sb.append(matcher.group(2) + " " 
                                + matcher.group(3)).append(System.lineSeparator());
                        }
                    }
                    entryMoves.put(key, sb.toString());
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            
        } catch(IOException e) {e.printStackTrace();}
    }

    public static String formatText(String text) {
        String[] words = text.split("_");
        StringBuilder formatted = new StringBuilder();

        for (String word : words) {
            formatted.append(Character.toUpperCase(word.charAt(0)))
                     .append(word.substring(1))
                     .append(" ");
        }
        return formatted.toString().trim();
    }

    private static String getValidMoves(String jarPath){
        String vm = "";
        Pattern p = Pattern.compile("move\\s+([\\w_]+),\\s*[\\w_]+,\\s*(\\d+),\\s*(\\w+)");
        Matcher m;
        try (ZipFile zf = new ZipFile(jarPath)){
            ZipEntry entry = zf.getEntry("pokemon/moves.asm");
            if (entry != null) {
                try (BufferedReader reader = 
                new BufferedReader(new InputStreamReader(zf.getInputStream(entry)))) {
                    String line;
                    boolean copyLines = false;                    
                    StringBuilder content = new StringBuilder();
                    while ((line = reader.readLine()) != null) {
                        m = p.matcher(line);
                        if (m.find()) {
                            copyLines = true;
                            content.append((m.group(1) + " " + m.group(2) 
                                        + " " + m.group(3)).trim()).append(System.lineSeparator());
                        } else if (copyLines && line.trim().isEmpty()) { break; }
                    }
                    vm = content.toString();
                    reader.close();
                }
            }
        } catch (IOException e) { e.printStackTrace(); }
        return vm;
    }

    public static void saveEdits(String newStats, String newMoves, HashMap<Integer, String> entries, 
                                HashMap<Integer,String> entryMoves, String modsPath,
                                List<Path> moveDirs, List<Path> statDirs, int idx) {
        //write changes in memory, save only at close
        //moveDirs.sort((o1, o2) -> o1.getFileName().compareTo(o2.getFileName()));
        //statDirs.sort((o1, o2) -> o1.getFileName().compareTo(o2.getFileName()));
        // Pattern to extract relevant parts, A - base_stats.asm, B - evos_attacks.asm
        Pattern patternA = Pattern.compile("(\\t(db|dn) )([\\w_, ]+)(; [\\w ]+)?");
        Pattern patternB = Pattern.compile("(\\tdb\\s*)([\\w_]+),\\s+([\\w_]+)([\\w,_ ]+)?");
        String[] target = {"base_stats.asm", "evos_attacks.asm"};
        //only want to write to files changed
        //read all lines in path changed, write to pattern matches only
        for(int j = 0; j < Math.max(statDirs.size(), moveDirs.size()); j++){
            //################
            //FINE. DO NOT TOUCH :) - 06/12/25
            //   vvvvvvvvvvvvvv
            //################
            if (statDirs.get(j) != null) {
                //Set<String> statString = new HashSet<>();
                //05/19/25 - FINAL STRETCH, FIX ACCESSDENIED EXCEPTION
                try (BufferedReader rd = new BufferedReader(
                    new StringReader(Files.readString(statDirs.get(j).resolve(target[0]))))) {
                    List<String> fileLines = Files.readAllLines(statDirs.get(j).resolve(target[0]));
                    //lookup in entries for statDirs next
                    //advancing based on loop, not path idx - 06/06/25
                    Iterator<String> h = entries.get(targetPaths.indexOf(statDirs.get(j))).lines().iterator();
                    StringBuilder sb = new StringBuilder();
                    BufferedWriter bw = Files.newBufferedWriter(statDirs.get(j).resolve(target[0]), 
                                        StandardOpenOption.WRITE);
                    //05/19/25 - FIX SYNTAX, CURRENT NOT PARSING PROPERLY, SEE MODS DIR
                    for (String line: fileLines) {
                        Matcher matcher = patternA.matcher(line);
                        if(matcher.find() && h.hasNext()) {
                            sb.append(matcher.group(1) + h.next());
                            if(matcher.group(4) != null){
                                sb.append(" " + matcher.group(4) + System.lineSeparator());
                            } else if(matcher.group(4) == null)
                                sb.append(System.lineSeparator());
                        }
                        //WHY DUPES OF 3 LINES -> group4 captured when and triggered
                        else if(!matcher.find()){ sb.append(line + System.lineSeparator()); }
                    }
                    //String t = statString.stream().collect(Collectors.joining(System.lineSeparator()));
                    bw.write(sb.toString(), 0, sb.toString().length()-1);
                    bw.close();
                    } catch (IOException e) { e.printStackTrace(); break;}
            }
            //################
            //FINE. DO NOT TOUCH :) - 06/12/25
            //   ^^^^^^^^^^^^^^
            //################
            if (moveDirs.get(j) != null) {
                try (BufferedReader rd = new BufferedReader(
                    new StringReader(Files.readString(moveDirs.get(j).resolve(target[1]))))) {
                    Iterator<String> fileLines = Files.readAllLines(moveDirs.get(j).resolve(target[1])).iterator();
                    Stream<String> h = entryMoves.get(targetPaths.indexOf(moveDirs.get(j))).lines();
                    StringBuilder sb = new StringBuilder();
                    BufferedWriter bw = Files.newBufferedWriter(moveDirs.get(j).resolve(target[1]), 
                                        StandardOpenOption.WRITE);
                    for (String line: h.toList()) {
                        //06/25/25 - FUNCTIONAL, FIX FORMATTING ISSUES ON MATCHES
                        //ternary operator checks for entryMoves.length > fileLines.length
                        String fileLine = fileLines.hasNext() ? fileLines.next() : null;
                        Matcher matcher = fileLine != null ? patternB.matcher(fileLine) : null;
                        if(matcher == null || !matcher.find()) {
                            sb.append(fileLine).append(System.lineSeparator());
                        }
                        else {
                            sb.append(matcher.group(1) + line.replace(" ", ", ") + System.lineSeparator()); 
                        }
                    }
                    bw.write(sb.toString(), 0, sb.toString().length()-1);
                    bw.close();
                } catch (IOException e) { e.printStackTrace(); break;}
            }
        }
        System.out.println("Files updated: " + statDirs.toString() + "\n" + moveDirs.toString());
    }

/*  public static void saveEdits(String newStats, String newMoves, HashMap<Integer, String> entries, 
                             HashMap<Integer,String> entryMoves, String modsPath,
                             List<Path> moveDirs, List<Path> statDirs, int idx) {
        Pattern patternA = Pattern.compile("(\\t(db|dn) )([\\w_, ]+)(; [\\w ]+)?");
        Pattern patternB = Pattern.compile("(\\tdb\\s*)([\\w_]+),\\s+([\\w_]+)([\\w,_ ]+)?");
        String[] target = {"base_stats.asm", "evos_attacks.asm"};
        
        for(int j = 0; j < Math.max(statDirs.size(), moveDirs.size()); j++) {
            if (statDirs.get(j) != null && entries.containsKey(j)) {
                processFile(statDirs.get(j).resolve(target[0]), patternA, entries.get(j));
            }
            if (moveDirs.get(j) != null && entryMoves.containsKey(j)) {
                processFile(moveDirs.get(j).resolve(target[1]), patternB, entryMoves.get(j));
            }
        }
    }

    private static void processFile(Path filePath, Pattern pattern, String newContent) {
        try {
            List<String> fileLines = Files.readAllLines(filePath);
            Iterator<String> newEntries = newContent.lines().iterator();
            Set<String> uniqueLines = new LinkedHashSet<>();
            
            for (String line : fileLines) {
                Matcher matcher = pattern.matcher(line);
                if (matcher.find() && newEntries.hasNext()) {
                    String updatedLine = matcher.group(1) + newEntries.next();
                    if (matcher.group(4) != null) {
                        updatedLine += " " + matcher.group(4);
                    }
                    uniqueLines.add(updatedLine);
                } else {
                    uniqueLines.add(line);
                }
            }
            
            Files.write(filePath, uniqueLines, StandardOpenOption.WRITE);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }*/

    /*Find directories missing base_stats.asm, read base_stats.asm from archive, write to respective directories*/
    public static void getTargetDirectories(String jarPath, String modsPath, List<Path> emptyDirs, 
                                            List<Path> moveDirs, List<Path> statDirs, String movesPath, 
                                            String statsPath, HashMap<Integer,BufferedImage> images, 
                                            HashMap<Integer,String> entries, HashMap<Integer,String> entryMoves)
                                            throws IOException {
        String[] target = {"base_stats.asm", "evos_attacks.asm", "front.png"};
        Stream<Path> fs = Files.list(Paths.get(modsPath))
            .filter(x -> Files.isDirectory(x, LinkOption.NOFOLLOW_LINKS))
            .sorted((o1, o2) -> o1.getFileName().compareTo(o2.getFileName()));
        targetPaths.addAll(fs.collect(Collectors.toList()));
        fs.close();
        BufferedImage b;

        try(ZipFile zf = new ZipFile(jarPath)){
            for(int key = 0; key < targetPaths.size(); key++){
                Path currPath = targetPaths.get(key);

                if (Files.notExists(currPath.resolve(target[0]))) {
                    emptyDirs.add(currPath);
                    System.out.println("Attempting update to stat tables from archive...\n" + statDirs);
                    appendEmptyStats(jarPath, statsPath, currPath, entries);
                    // want to make currPath point to newly created base_stats.asm                    
                    updateEntries(key, currPath.resolve(target[0]), entries, entryMoves);
                } else if (Files.exists(currPath.resolve(target[0])))
                    updateEntries(key, currPath.resolve(target[0]), entries, entryMoves);

                if (Files.notExists(currPath.resolve(target[1]))) {
                    emptyDirs.add(currPath);
                    System.out.println("Attempting update to moves from archive...\n" + moveDirs);
                    appendEmptyMoves(jarPath, movesPath, currPath, entryMoves);
                    updateEntries(key, currPath.resolve(target[0]), entries, entryMoves);
                } else if (Files.exists(currPath.resolve(target[1])))
                    updateEntries(key, currPath.resolve(target[1]), entries, entryMoves);

                if (Files.exists(currPath.resolve(target[2]))) {
                    b = ImageIO.read(currPath.resolve(target[2]).toFile());
                    // b = resize(b, 1.5, 1.5);
                    images.put(key, b.getSubimage(0, 0, b.getWidth(), b.getWidth()));
                }
                if (Files.notExists(currPath.resolve(target[2]))) {
                    // check if front.png in currPath, ref .jar if not found
                    String t = currPath.getFileName().toString();
                    ZipEntry jarEntry = zf.getEntry("pokemon/pokemon/" + t + "/front.png");
                    // if front.png not found in default directory, source from credited directory
                    if (jarEntry == null) {
                        jarEntry = zf.getEntry("pokemon/credited/pokemon/" + t + "/front.png");
                    }
                    b = ImageIO.read(zf.getInputStream(jarEntry));
                    //b = resize(b, 1.5, 1.5);
                    images.put(key, b.getSubimage(0, 0, b.getWidth(), b.getWidth()));
                }
            }
        } catch(IOException e) { e.printStackTrace(); }
    }

    /**
    * Resizes an Image by stretching it to a new size by x factor and y factor.
    *
    * @param picture the initial image.
    * @param xFactor the width stretching factor.
    * @param yFactor the height stretching factor.
    * @return the stretched image.
    */
   public static Image resize(Image picture, double xFactor, double yFactor)
   {
      BufferedImage buffer;
      Graphics2D g;
      AffineTransform transformer;
      AffineTransformOp operation;

      buffer = new BufferedImage
      (
         picture.getWidth(null),
         picture.getHeight(null),
         BufferedImage.TYPE_INT_ARGB
      );
      g = buffer.createGraphics();
      g.drawImage(picture, 0, 0, null);
      transformer = new AffineTransform();
      transformer.scale(xFactor, yFactor);
      operation = new AffineTransformOp(transformer, AffineTransformOp.TYPE_BILINEAR);
      buffer = operation.filter(buffer, null);
      //return buffer;
      return(Toolkit.getDefaultToolkit().createImage(buffer.getSource()));
   }
}
