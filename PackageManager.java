import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.Stack;
import org.json.simple.parser.ParseException;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Iterator;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.*;

/**
 * Filename: PackageManager.java Project: p4 Authors: Shefali Mukerji
 * 
 * PackageManager is used to process json package dependency files and provide function that make
 * that information available to other users.
 * 
 * Each package that depends upon other packages has its own entry in the json file.
 * 
 * Package dependencies are important when building software, as you must install packages in an
 * order such that each package is installed after all of the packages that it depends on have been
 * installed.
 * 
 * For example: package A depends upon package B, then package B must be installed before package A.
 * 
 * This program will read package information and provide information about the packages that must
 * be installed before any given package can be installed. all of the packages in
 * 
 * You may add a main method, but we will test all methods with our own Test classes.
 */

public class PackageManager {

  private Graph graph;
  private Package[] pack;

  /*
   * Package Manager default no-argument constructor.
   */
  public PackageManager() {
    graph = new Graph();
    pack = null;
  }

  /**
   * Takes in a file path for a json file and builds the package dependency graph from it.
   * 
   * @param jsonFilepath the name of json data file with package dependency information
   * @throws FileNotFoundException if file path is incorrect
   * @throws IOException           if the give file cannot be read
   * @throws ParseException        if the given json cannot be parsed
   */
  public void constructGraph(String jsonFilepath)
      throws FileNotFoundException, IOException, ParseException {
    Object obj = new JSONParser().parse(new FileReader(jsonFilepath));
    JSONObject jo = (JSONObject) obj;
    JSONArray packages = (JSONArray) jo.get("packages");

    pack = new Package[packages.size()]; // array of Packages(GraphNodes) from JSON
    for (int i = 0; i < packages.size(); ++i) {
      JSONObject jsonPack = (JSONObject) packages.get(i); // creates JSONObject for each package in
                                                         // file
      String vertex = (String) jsonPack.get("name"); // extracts the name of package and stores it
                                                     // in variable vertex
      graph.addVertex(vertex); // adds vertex to graph

      JSONArray jsonDep = (JSONArray) jsonPack.get("dependencies");
      int num = (int) jsonDep.size();
      for (int j = 0; j < num; ++j) {
        String depVert = (String) jsonDep.get(j);
        graph.addEdge(vertex, depVert);
      }
    }
  }

  /**
   * Helper method to get all packages in the graph.
   * 
   * @return Set<String> of all the packages
   */
  public Set<String> getAllPackages() {
    return graph.getAllVertices();
  }

  /**
   * Given a package name, returns a list of packages in a valid installation order.
   * 
   * Valid installation order means that each package is listed before any packages that depend upon
   * that package.
   * 
   * @return List<String>, order in which the packages have to be installed
   * 
   * @throws CycleException           if you encounter a cycle in the graph while finding the
   *                                  installation order for a particular package. Tip: Cycles in
   *                                  some other part of the graph that do not affect the
   *                                  installation order for the specified package, should not throw
   *                                  this exception.
   * 
   * @throws PackageNotFoundException if the package passed does not exist in the dependency graph.
   */
  public List<String> getInstallationOrder(String pkg)
      throws CycleException, PackageNotFoundException {
    Set set = graph.getAllVertices();
    if (!set.contains(pkg)) {
      throw new PackageNotFoundException();
    }
    List<String> order = new ArrayList<String>();
    if (hasCycle(pkg)) {
      throw new CycleException();
    }
    getInstallHelper(pkg, order);
    return order;

  }

  /**
   * Given two packages - one to be installed and the other installed, return a List of the packages
   * that need to be newly installed.
   * 
   * 
   * @return List<String>, packages that need to be newly installed.
   * 
   * @throws CycleException           if you encounter a cycle in the graph while finding the
   *                                  dependencies of the given packages. If there is a cycle in
   *                                  some other part of the graph that doesn't affect the parsing
   *                                  of these dependencies, cycle exception should not be thrown.
   * 
   * @throws PackageNotFoundException if any of the packages passed do not exist in the dependency
   *                                  graph.
   */
    public List<String> toInstall(String newPkg, String installedPkg)
      throws CycleException, PackageNotFoundException {
      Set set = graph.getAllVertices();
      if (!set.contains(newPkg)) {
        throw new PackageNotFoundException();
      }
      if (!set.contains(installedPkg)) {
        throw new PackageNotFoundException();
      }
      if (hasCycle(newPkg)) {
        throw new CycleException();
      }
      if (hasCycle(installedPkg)) {
        throw new CycleException();
      }
      List<String> newPkgAdj = new ArrayList<String>();
      List<String> installedAdj = new ArrayList<String>();
      List<String> toInstall = new ArrayList<String>();

      newPkgAdj = getChainDependencies(newPkg, newPkgAdj);
      installedAdj = getChainDependencies(installedPkg, installedAdj);

      toInstall.add(newPkg);
      for (int i = 0; i < newPkgAdj.size(); ++i) {
        if (!installedAdj.contains(newPkgAdj.get(i))) {
          toInstall.add(newPkgAdj.get(i));
        }
      }
      toInstall.remove(installedPkg);
      return toInstall;
    }

  /**
   * Return a valid global installation order of all the packages in the dependency graph.
   * 
   * 
   * @return List<String>, order in which all the packages have to be installed
   * @throws CycleException if you encounter a cycle in the graph
   */
  public List<String> getInstallationOrderForAllPackages() throws CycleException {
    if (hasCycle()) {
      throw new CycleException();
    }
    Set set = graph.getAllVertices();
    List<String> order = new ArrayList<String>();
    for (Object vertex : set) {
      getInstallHelper((String) vertex, order);
    }
    return order;
  }

  private void getInstallHelper(String vertex, List<String> order) {
    List<String> temp = graph.getAdjacentVerticesOf(vertex);
    if (temp.isEmpty()) {
      if (!order.contains(vertex)) {
        order.add(vertex);
      }
    } else {
      for (int i = 0; i < temp.size(); ++i) {
        getInstallHelper(temp.get(i), order);
      }
      if (!order.contains(vertex)) {
        order.add(vertex);
      }
    }
  }


  /**
   * Find and return the name of the package with the maximum number of dependencies.
   *
   * @return String, name of the package with most dependencies.
   * @throws CycleException if you encounter a cycle in the graph
   */
  public String getPackageWithMaxDependencies() throws CycleException {
    if (hasCycle()) {
      throw new CycleException();
    }
    Object [] set =  graph.getAllVertices().toArray();
    String max = (String) set[0];
    for (int i = 1; i < set.length; ++i) {
      if (getNumDependencies((String) set[i]) > getNumDependencies(max)) {
        max = (String) set[i];
      }
    }
    return max;
  }

  public static void main(String[] args) {
    System.out.println("PackageManager.main()");
  }

  private boolean hasCycle() {
    Set set = graph.getAllVertices();
    for (Object vertex : set) {
      if (hasCycle((String) vertex)) {
        return true;
      }
    }
    return false;
  }

  private boolean hasCycle(String vertex) {
    List<String> dep = new ArrayList<String>();
    dep = getChainDependencies(vertex, dep);
    for (int i = 0; i < dep.size(); ++i) {
      if (dep.get(i).compareTo(vertex) == 0) {
        return true;
      }
    }
    return false;
  }


  private int getNumDependencies(String vertex) {
    int dep = 0;
    List<String> adj = graph.getAdjacentVerticesOf(vertex);
    if (adj.isEmpty()) {
      return dep;
    } else {
      dep = dep + adj.size();
      for (String vert : adj) {
        dep += numDepHelper(vert, adj);
      }
    }
    return dep;
  }

  private int numDepHelper(String vert, List<String> adj) {
    int dep = 0;
    List<String> temp = graph.getAdjacentVerticesOf(vert);
    if (temp.isEmpty()) {
      if (!adj.contains(vert)) {
        dep++;
      }
    } else {
      for (String vertex : temp) {
        numDepHelper(vertex, temp);
      }
    }
    return dep;
  }

  private List<String> getChainDependencies(String vertex, List<String> dep) {
    List<String> adj = graph.getAdjacentVerticesOf(vertex);
    if (adj.isEmpty()) {
      return dep;
    }
    for (int i = 0; i < adj.size(); ++i) {
      if (!dep.contains(adj.get(i))) {
        dep.add(adj.get(i));
        getChainDependencies(adj.get(i), dep);
      }
    }
    return dep;
  }



}
