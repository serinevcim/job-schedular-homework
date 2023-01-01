import java.util.*;
import java.io.*;
/*
Serin Evcim
211401015
*/
//*******************************************************CLASS JOBSCHEDULAR ********************************************************/

public class JobScheduler {

    /* instances:
     * timer            :keeps the time
     * jobFilePath      :file path for the file which includes information about jobs
     * reader           :file reader to read jobFilePath
     * schedularTree    :Node based minHeap, keeps jobs according to their arrival time
     * jobs             :all jobs that jobFilePath icludes, i used it in dependencyBlockedJobs() and resourceBlockedJobs(), set it in insertJob()
     * completedJobs    :keeps completed jobs, set it at the beggining of run
     * allTimeLineOutput:every element of this arrayList keeps an array whose length is resourceCount, every element's index is current timer,
     *                   these arrays save the jobs that currently working at i'th resource to i'th index, set it in the end of run
     * dependencyBlocked:keeps the dependent jobs temporarily while inserting jobs to resources
     * resources        :keeps resources
     * resourceCount    :count of resources
     * dependencyMap    :maps the job id to the arrayList which stores the id's of jobs that create dependency for the job
     */

    public Integer timer = 0;
    public String jobFilePath;
    Scanner reader;

    public MinHeap<Job> schedulerTree;
    ArrayList<Job> jobs;
    ArrayList<Job> completedJobs;
    ArrayList<Job[]> allTimeLineOutput; 
    ArrayList<Job> dependencyBlocked;

    ArrayList<Resource> resources;
    int resourceCount;
    HashMap<Integer, ArrayList<Integer>> dependencyMap;


    //----------------------------------------CONSTRUCTOR--------------------------------------------
    public JobScheduler(String jfp) {
        jobFilePath = jfp;
        try{
            reader = new Scanner(new File(jobFilePath));
        }catch(FileNotFoundException e){
            System.out.println(e.getMessage());
        }
        schedulerTree = new MinHeap<>();
        jobs = new ArrayList<>();
        completedJobs = new ArrayList<>();
        allTimeLineOutput = new ArrayList<>();
        dependencyBlocked = new ArrayList<>();
        dependencyMap = new HashMap<>();
    }




    //---------------------------------------GIVEN METHODS--------------------------------------------
    /* reads given file line by line and stores the integer in splittedInt respectively
     * hashMap consists of an integer as key(depended job's id) and an value arraylsist(job ids that create dependency)
     * stores these integers to hashMap, first element is key and the other is an element of arraylist
     * if key exists in hashMap then value is added to the arraylist of that key
     */
    public void insertDependencies(String dependencyPath){
        File file = new File(dependencyPath);
        String[] splitted = new String[2];
        int[] splittedInt = new int[2];
        try{
            Scanner fileReader = new Scanner(file);
            while(fileReader.hasNextLine()){
                splitted = (fileReader.nextLine()).split(" ", 2);
                for(int i=0; i<2; i++){
                    splittedInt[i] = Integer.parseInt(splitted[i]);
                }
                if(dependencyMap.containsKey(splittedInt[0])){
                    dependencyMap.get(splittedInt[0]).add(splittedInt[1]);
                }else{
                    ArrayList<Integer> arrayList = new ArrayList<>();
                    arrayList.add(splittedInt[1]);
                    dependencyMap.put(splittedInt[0], arrayList);
                }
            }
            fileReader.close();
        }catch (FileNotFoundException e) {
            System.out.println(e.getMessage());
        }
    }

    /* checks the jobFilePath, if the reader has next line returbs true otherwise false
     */
    public boolean stillContinues(){
        if(reader.hasNextLine())
            return true;
        return false;
    }

    /* first, checks the resources, if there is a finished job saves it to an arrayList and removes it from resource
     * then, inserts a job from schedularTree to the resources
     * finally, save the currently working jobs to allTimeLineOutput
    */
    public void run(){
        // checking completed jobs and saving them to arraylist
        for(int i=0; i<resourceCount; i++){
            if(resources.get(i).currentJob!= null){
                if(resources.get(i).currentJob.getDuration()==0){
                    completedJobs.add(resources.get(i).currentJob);
                    resources.get(i).currentJob = null;
                }
            }
        }

        // inserting jobs to resources or decreasing durations of working jobs
        // when the duration is 0 job is completed and its checken above
        for(int i=0; i<resourceCount; i++){
            if(resources.get(i).currentJob == null){ //then we will insert the min of schedularTree
                if(schedulerTree.root!=null){
                    while(isDependend(schedulerTree.min())){
                        /* if root(min) of scheduarTree is dependent we will save it to an arrayList,
                         * remove it from schedularTree and
                         * try to insert the new root (recheck if it is dependent either - loop)
                        */
                        dependencyBlocked.add(schedulerTree.remove());
                    }
                }
                    
                // the root of schedularTree is no longer dependent so we inserted it
                resources.get(i).currentJob = schedulerTree.remove(); 

                // reinsert the jobs we saved to arrayList above
                for(int j=0; j<dependencyBlocked.size();j++){
                    schedulerTree.add(dependencyBlocked.get(j));
                }
                dependencyBlocked = new ArrayList<>(); //for resetting the arraylist
            }else{//there is a job working already so we decrease the duration
                resources.get(i).currentJob.setDuration(resources.get(i).currentJob.getDuration() - 1);
            }
        }

        //setting allTimeLineOutput
        Job[] currentlyWorkingJobs = new Job[resourceCount];
        for(int i=0; i<resourceCount; i++)
            currentlyWorkingJobs[i] = resources.get(i).currentJob;
        allTimeLineOutput.add(currentlyWorkingJobs);
    }
    
    /* sets the resourceCount by count and,
     * creates 'resources' arraylist according to given resourceCount
     */
    public void setResourcesCount(Integer count){
        Resource newResource;
        resources = new ArrayList<>();
        for(int i=0; i<count; i++){
            newResource = new Resource(i);
            resources.add(newResource);
        }
        resources.trimToSize();
        resourceCount = count;
    }

    /* if jobFilePath's line includes information about a job, reads it and creates a job
     * then, inserts the job to the arrayList 'jobs' and schedularTree
     * increases timer
     */
    public void insertJob(){
        String line = reader.nextLine();
        if(!line.equals("no job")){
            String[] splitted = line.split(" ", 2);
            Job newJob = new Job(timer, Integer.parseInt(splitted[0]), Integer.parseInt(splitted[1])-1);
            jobs.add(newJob);
            schedulerTree.add(newJob);
        }
        timer++;
    }

    /* prints the completed jobs by looking at the arrayList completedJobs 
     */
    public void completedJobs(){
        System.out.print("completed jobs: ");
        for(int k=0; k<completedJobs.size(); k++){
            if(k!=0) System.out.print(",");
            System.out.print(completedJobs.get(k));    
        }
        System.out.println("\n");
    }

    /* prints the dependency blocked jobs by looking at the arrayList 'jobs' 
     * if a job from this arrayList arrived, inserted to the schedularTree and dependent then, prints
     */
    public void dependencyBlockedJobs(){
        System.out.print("dependency blocked jobs: ");
        for(int k=0; k<jobs.size(); k++){
            if((jobs.get(k).getArrivalTime() <= timer) && (schedulerTree.jobSet.contains(jobs.get(k))) && (isDependend(jobs.get(k))))
                if(dependencyMap.containsKey(jobs.get(k).getId()))
                    System.out.print("(" + jobs.get(k) + "," + dependencyMap.get(jobs.get(k).getId()).get(stopper(dependencyMap.get(jobs.get(k).getId()))) + ")");    
        }
        System.out.println("\n");
    }

    /* prints the resource blocked jobs by looking at thr arrayList 'jobs'
     * if a job from this arrayList arrived, inserted to the schedularTree but not depended then, prints
     */
    public void resourceBlockedJobs(){
        boolean printedTheFirstResourceBlocked = false; // for making a nice looking when using ','
        System.out.print("resource blocked jobs: ");
        for(int k=0; k<jobs.size(); k++){
            if((jobs.get(k).getArrivalTime() <= timer) && (schedulerTree.jobSet.contains(jobs.get(k))) && (!isDependend(jobs.get(k)))){
                if(printedTheFirstResourceBlocked) System.out.print(",");
                System.out.print(jobs.get(k));
                printedTheFirstResourceBlocked = true;
            }    
        }
        System.out.println("\n");
    }

    /* printd the working jobs by looking at the arrayList 'resources'
     * reaches the currentJob' of resources and prints
     */
    public void workingJobs(){
        System.out.print("working jobs: ");
        for( int i=0; i<resourceCount; i++){
            if(resources.get(i).currentJob != null)
                System.out.print("(" + resources.get(i).currentJob.getId() + "," + (resources.get(i).resourceId+1) + ")");
        }
        System.out.println();
    }

    /* calls run as long as there are jobs remaining in the schedularTree or in resources
     * increases the timer
     */
    public void runAllRemaining(){
        while((schedulerTree.size != 0) || !areAllResourcesEmpty()){
            this.run();
            timer++;
        }
    }
    
    /* printd the all time line by looking at the arrayList 'allTimeLineOutput'
     */
    public void allTimeLine(){
        for(int i=0; i<resourceCount; i++){
            System.out.print("     R" + (i+1) + "  ");
        }
        System.out.println();
        for(int i=0; i<allTimeLineOutput.size(); i++){
            if(allTimeLineOutput.get(i)!= null){
                System.out.print("" + i);
                for(int k=0; k<resourceCount; k++){    
                    System.out.print("    " + allTimeLineOutput.get(i)[k] + "    ");
                }
            System.out.println();
            }
        }
    }

    /* calls the toString method of MinHeap */
    public String toString(){
        return schedulerTree.toString();
    }




//--------------------------------METHODS THAT I ADDED - PRİVATE METHODS----------------------------------
    /* returns true if the job given as parameter is depended, false otherwise
     * called by run(), dependencyBlockedJobs() and resourceBlockedJobs()
     */
    private boolean isDependend(Job j){
        if(j==null) return false;
        if(j.getArrivalTime()<timer){
            if(dependencyMap.containsKey(j.getId())){
                if(isAllStopperJobsFinished(dependencyMap.get(j.getId()))){
                    return false;
                }
                return true;
            }
            return false;
        }
        return true;
    }
    
    /* returnt true if all of the jobs in arrayList given as parameter (which contains job id's that create dependency) are completed, false otherwise
     * called by isDependent()
     */
    private boolean isAllStopperJobsFinished(ArrayList<Integer> j){
        boolean flag = true;
        if(j!= null){
            for(int k=0; k<j.size(); k++){
                if(j.get(k)!= null){
                    if(!isCompleted(j.get(k))){
                        flag = false;
                    }
                }
            }
        }
        return flag;
    }
    
    /* returns true if the job which has the id given as paremeter is completed, false otherwise
     * called by stopper()
     */
    private boolean isCompleted(int k){
        for( int i=0; i<completedJobs.size(); i++){
            if(completedJobs.get(i).getId() == k)
                return true;
        }
        return false;
    }

    /* returns the index of the stopper job (the job that creates dependency) in the arrayList given as parameter,
     * returns 0 if arraylists size is 0
     * called by dependencyBlockedJobs()
     */
    private int stopper(ArrayList<Integer> dep){
        for(int i=0; i<dep.size(); i++){
            if(!isCompleted(dep.get(i)))
                return i;
        }
        return 0;
    }

    /*returns true if all resources are empty, false otherwise
     * called by runAllRemaining()
     */
    private boolean areAllResourcesEmpty(){
        for(int i=0; i<resourceCount; i++){
            if(resources.get(i)!= null){
                if(resources.get(i).currentJob != null){
                    return false;
                }
            }
        }
        return true;
    }




    //----------------------------------------------------INNER CLASSES---------------------------------------------------

    public class Resource {
        /* currentJob   :stores the inserted job
         * resourceId   :resource's id
         */
        Job currentJob;
        int resourceId;
        
        //--------CONSTRUCTOR
        public Resource(int id){
            resourceId = id;
            currentJob = null;
        }
    }

    public class Job implements Comparable<Job>{
        /* arrivalTime  :the line that job has read from
         * id           :job's id
         * duration     :the amount of time that job must stay in a resource
         */
        private int arrivalTime;
        private int id;
        private int duration;
        
        //--------CONSTRUCTOR
        public Job(int a, int i, int d){
            arrivalTime = a;
            id = i;
            duration = d;
        }

        //--------METHODS

        //if a swap is needed in minHeapify returns 1 else 0
        public int compareTo(Job o){ // 'this' will be the LastNode at the first call
            if(this.arrivalTime < o.arrivalTime)
                return 1;
            return 0;
        }

        //returns the id as string
        public String toString(){
            return String.valueOf(id);
        }

        // returns true if this and object given as parameter has the same id, false otherwise
        public boolean equals(Object o){
            Job j = (Job) o;
            if(this.id == j.id)
                return true;
            return false;
        }

        //--------GETTERS AND SETTERS
        public int getArrivalTime() {
            return arrivalTime;
        }
        public void setArrivalTime(int arrivalTime) {
            this.arrivalTime = arrivalTime;
        }
        public int getId() {
            return id;
        }
        public void setId(int id) {
            this.id = id;
        }
        public int getDuration() {
            return duration;
        }
        public void setDuration(int duration) {
            this.duration = duration;
        }
    }

    public class MinHeap<E extends Comparable<E>> {
        /* root         :root of the tree
         * LastAdded    :the node en of the tree
         * size         :number of the nodes tree has
         * jobSet       :jobs that tree includes currently
         */
        Node<E> root;
        Node<E> LastAdded;
        int size;
        ArrayList<Job> jobSet;

        //--------CONSTRUCTOR
        MinHeap(){
            root = new Node<E>();
            LastAdded = root;
            size = 0;
            jobSet = new ArrayList<>();
        }

        //--------METHODS

        //returns the root since this is a minHeap
        E min(){
            if(size==0) return null;
            return this.root.getElement();
        }
         
        // adds a job which has the element given as parameter to the tree(further explanation is inside the method)
        void add(E e){
            if(size == 0){
                Node<E> newn = new Node<>(e);
                root = newn;
                jobSet.add((Job)e);
                LastAdded = root;
                size++;
            }
            else if(size == 1){
                if(!root.hasLeftClild()){ //add left child if it does not exists, add right child otherwise
                    root.addLeftChild(e);
                    root.leftChild.setParent(root);
                    jobSet.add((Job)e);
                    LastAdded = root.leftChild;
                    size++;
                }else if(!root.hasRightClild()){
                    root.addRightChild(e);
                    root.rightChild.setParent(root);
                    jobSet.add((Job)e);
                    LastAdded = root.rightChild;
                    size++;
                }
            }else{
                Node<E> ptr = LastAdded.parent; //LastAdded is a leaf, get its parent
                if(!ptr.hasLeftClild()){ //add left child if it does not exists, add right child otherwise
                    ptr.addLeftChild(e);
                    ptr.leftChild.setParent(ptr);
                    jobSet.add((Job)e);
                    LastAdded = ptr.leftChild;
                    size++;
                }else if(!ptr.hasRightClild()){
                    ptr.addRightChild(e);
                    ptr.rightChild.setParent(ptr);
                    jobSet.add((Job)e);
                    LastAdded = ptr.rightChild;
                    size++;
                }else{
                    /* go up until came on a left child
                    * if we have reached the root that means root's right side subtree is full
                    * then we are going start filling new line aka: height of tree increases 1
                    * so we go left until we reach a leaf then we can add a new node
                    * if we didnt reached the roof that means the subtree (which has current node as its top node) is full
                    * so we go the sibling of current node and start to fill there
                    */
                    while(!ptr.isRightChild()){
                        ptr = ptr.parent;
                    }
                    if(ptr == root){ // NEEDS CHECKİNG!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
                        while(ptr.isLeaf()){
                            ptr = ptr.leftChild;
                        }
                        if(!ptr.hasLeftClild()){
                            ptr.addLeftChild(e);
                            ptr.leftChild.setParent(ptr);
                            jobSet.add((Job)e);
                            LastAdded = ptr.leftChild;
                            size++;
                        }else if(!ptr.hasRightClild()){
                            ptr.addRightChild(e);
                            ptr.rightChild.setParent(ptr);
                            jobSet.add((Job)e);
                            LastAdded = ptr.rightChild;
                            size++;
                        }
                    }else{
                        ptr = ptr.parent;
                        ptr = ptr.rightChild;
                        if(ptr.isLeaf()){
                            if(!ptr.hasLeftClild()){
                                ptr.addLeftChild(e);
                                ptr.leftChild.setParent(ptr);
                                jobSet.add((Job)e);
                                LastAdded = ptr.leftChild;
                                size++;
                            }else if(!ptr.hasRightClild()){
                                ptr.addRightChild(e);
                                ptr.rightChild.setParent(ptr);
                                jobSet.add((Job)e);
                                LastAdded = ptr.rightChild;
                                size++;
                            }
                        }else{
                            while(ptr.isLeaf()){
                                ptr = ptr.leftChild;
                            }
                            if(!ptr.hasLeftClild()){
                                ptr.addLeftChild(e);
                                ptr.leftChild.setParent(ptr);
                                jobSet.add((Job)e);
                                LastAdded = ptr.leftChild;
                                size++;
                            }else if(!ptr.hasRightClild()){
                                ptr.addRightChild(e);
                                ptr.rightChild.setParent(ptr);
                                jobSet.add((Job)e);
                                LastAdded = ptr.rightChild;
                                size++;
                            }
                        }
                    }
                }
            }

            //upheap
            minHeapify(LastAdded);
        }
        
        // removes the root - min
        E remove(){
            E removed = null;
            if(size == 0){
                return removed;
            }else if(size == 1){
                removed = root.element;
                root = null;
                jobSet.remove(jobSet.indexOf(removed));
                size--;
            }
            else{
                removed = root.element;
                root.element = LastAdded.element;
                if(LastAdded.isRightChild()){
                    LastAdded.parent.rightChild = null;
                    jobSet.remove(jobSet.indexOf(removed));
                    LastAdded = this.getLastAdded();
                    size--;
    
                }else{
                    LastAdded.parent.leftChild = null;
                    jobSet.remove(jobSet.indexOf(removed));
                    LastAdded = this.getLastAdded();
                    size--;
                }
            }

            //downheap
            if(root != null)
                minHeapifyFromRoof(root);
            
            return removed;
        }
        
        //works as upHeap()
        private void minHeapify(Node<E> n){ // n wil be LastAdded for the first time it called
            if(n != root){
                if(n.element.compareTo(n.parent.element) == 1){
                    E temp = n.parent.element;
                    n.parent.element = n.element;
                    n.element = temp;
                    minHeapify(n.parent);
                }
            }
        }
        
        //works as downHeap()
        private void minHeapifyFromRoof(Node<E> n){ //n will be the roof when it first called
            if(n.leftChild != null && n.rightChild != null){
                if(n.leftChild.element.compareTo(n.rightChild.element)==1 && n.leftChild.element.compareTo(n.element)==1){
                    E temp = n.leftChild.element;
                    n.leftChild.element = n.element;
                    n.element = temp;
                    minHeapifyFromRoof(n.leftChild);
                }
                else if(n.rightChild.element.compareTo(n.leftChild.element)==1 && n.rightChild.element.compareTo(n.element)==1){
                    E temp = n.rightChild.element;
                    n.rightChild.element = n.element;
                    n.element = temp;
                    minHeapifyFromRoof(n.rightChild);
                }
            }
            else if(n.leftChild == null && n.rightChild != null && n.rightChild.element.compareTo(n.element) == 1){
                E temp = n.rightChild.element;
                n.rightChild.element = n.element;
                n.element = temp;
                minHeapifyFromRoof(n.rightChild);
            }
            else if(n.rightChild == null && n.leftChild != null &&n.leftChild.element.compareTo(n.element) == 1){
                E temp = n.leftChild.element;
                n.leftChild.element = n.element;
                n.element = temp;
                minHeapifyFromRoof(n.leftChild);
            }
        }

        //returns a string for current tree (further explanation is inside the method)
        public String toString(){
            ArrayList<Node<Job>> treesLine = new ArrayList<>(1);
            ArrayList<Node<Job>> tempTreesLine;
            /* these arrayLists are the lines of our tree to print it line by line,
             * first treesLine only contains the root: h=1 and do the appending
             * then we set tempTreesLine, it contains treesLine's child nodes: h=2
             * now, we can assign tempTreesLine to the treesLine and do appending in loop
             */
            StringBuilder str = new StringBuilder();
            treesLine.add((Node<Job>)this.root);
            str.append("     ");
            if(treesLine.get(0) != null)
                str.append(treesLine.get(0).element.getId());
            str.append("\n");
            if(treesLine.get(0)!= null){
                while(treesLine.get(0).leftChild!= null){
                    tempTreesLine = new ArrayList<>(treesLine.size()*2);
                    for(int i=0; i<treesLine.size(); i++){
                        tempTreesLine.add(treesLine.get(i).leftChild);
                        tempTreesLine.add(treesLine.get(i).rightChild);
                    }
                    treesLine = tempTreesLine;
                    for(int i=0; i<treesLine.size(); i++){
                        if(treesLine.get(i) != null)
                            str.append(treesLine.get(i).element.getId());
                        str.append("         ");
                    }
                    str.append("\n");
                }
            }
            return str.toString();
        }
    
        /* i will give an example to explain the algorithm i created here:
         * lets assume we have a tree that has 26 nodes it looks like this:
         *                                         node amount in line              total node count until line
         *                0                         2^0 = 1                         1 = 2^1 -1
         *        o               0                 2^1 = 2                         3 = 2^2 -1
         *    o       o       0       o             2^2 = 4                         7 = 2^3 -1
         *  o   o   o   o   o   0   o   o           2^3 = 8                        15 = 2^4 -1
         * o o o o o o o o o o 0                    11     (max: 2^4 = 16)         11         (max: 2^5 -1)
         * 
         * now, using the information i wrote above lets find the last node strarting from root:
         * i used ' and '' as flags
         * size:26      --> the biggest 2^k is:2^4
         *                  26 = (2^4-1) + 11' = 15 + 11'       --> 11'>2^3 GO RIGHT (recalculate the size by this condition)
         *                                                          new size is: 11'-1 = 10
         * size:10      --> the biggest 2^k is:2^3
         *                  10 = (2^3-1) + 3 = 7' + 3''         --> 3''<=2^2 GO LEFT
         *                                                          new size is: [(7'-1)/2]+3'' = 6
         * size:6       --> the biggest 2^k is:2^2
         *                  6 = (2^2-1) + 3 = 3 + 3'            --> 3'>2^1 GO RIGHT
         *                                                          new size is: 3'-1 = 2
         * size:2       --> the biggest 2^k is:2^1
         *                  2 = (2^1-1) + 1 = 1' + 1''          --> 1''<=2^0 GO LEFT
         *                                                          nwe size is: [(1'-1)/2]+1'' = 1
         * we found the last node!
         */
        public Node<E> getLastAdded(){
            Node<E> walker = (Node<E>)root;
            int sizeT = this.size;
            int iterator = (int)Math.log(sizeT);
            int leftTo = -1;
            int rightTo = -1;
            for(int i = iterator ; i > 0 ; i--){
                leftTo = (int)Math.pow(2, i) - 1;
                rightTo = (int)(sizeT - Math.pow(2, i) - 1);
                if(rightTo <= (int)Math.pow(2,iterator-1)){
                    walker = walker.leftChild;
                    sizeT = ((leftTo-1)/2) + rightTo; 
                }
                else{
                    walker = walker.rightChild;
                    sizeT = rightTo-1;
                }
            }
            return walker;
        }
        
        //--------INNER CLASS NODE
        public class Node<E>{
            private Node<E> rightChild;
            private Node<E> leftChild;
            private Node<E> parent;
            private E element; //Job

            //--------CONSTRUCTORS
            Node(){
                rightChild = null;
                leftChild = null;
                parent = null;
                element = null;
            }
            Node(E elmnt){
                rightChild = null;
                leftChild = null;
                parent = null;
                element = elmnt;
            }

            //--------METHODS
            void addRightChild(E e){
                Node<E> n = new Node<E>(e);
                rightChild = n;
                n.setParent(this);
            }
            void addLeftChild(E e){ 
                Node<E> n = new Node<E>(e);
                leftChild = n;
                n.setParent(this);
            }
            boolean hasRightClild(){
                return ((rightChild==null) ? false:true);
            }
            boolean hasLeftClild(){
                return ((leftChild==null) ? false:true);
            }
            boolean isRightChild(){
                if(this == root) return false;
                Node<E> p = this.parent;
                if(p.rightChild != null){
                    if(p.rightChild == this)
                        return true;
                    return false;
                }
                return false;
            }
            private boolean isLeaf(){
                if((rightChild == null) && (leftChild == null))
                    return true;
                return false;
            }
    
            //--------GETTERS AND SETTERS
            public Node<E> getRightChild() {
                return rightChild;
            }
            public void setRightChild(Node<E> rightChild) {
                this.rightChild = rightChild;
            }
            public Node<E> getLeftChild() {
                return leftChild;
            }
            public void setLeftChild(Node<E> leftChild) {
                this.leftChild = leftChild;
            }
            public Node<E> getParent() {
                return parent;
            }
            public void setParent(Node<E> parent) {
                this.parent = parent;
            }
            public E getElement() {
                return element;
            }
            public void setElement(E element) {
                this.element = element;
            }
        }
    }

    public class ArrayList<K>{
        private int defaultLength = 10;
        private int size;
        private transient K[] data;
        public ArrayList(){
            data = (K[]) new Object[defaultLength]; 
        }
        public ArrayList(int length){
            data = (K[]) new Object[defaultLength]; 
            defaultLength = length;
        }
    
        //METHODS:
        public void trimToSize(){
            if (size != data.length){
                K[] newData = (K[]) new Object[size];
                System.arraycopy(data, 0, newData, 0, size);
                data = newData;
            }
        }
        public int size(){
            return size;
        }
        public boolean isEmpty(){
            return size == 0;
        }
        public K get(int index){
            checkBoundExclusive(index);
            return data[index];
        }
        public K set(int index, K e)
        {
            checkBoundExclusive(index);
            K result = data[index];
            data[index] = e;
            return result;
        }
        public boolean add(K e){
            if(size==data.length){
                K[] newData = (K[]) new Object[defaultLength*2];
                defaultLength *= 2;
                System.arraycopy(data, 0, newData, 0, size);
                data = newData;
                data[size] = e;
            }
            else data[size] = e;
            size++;
            return true;
        }
        public K remove(int index){
            checkBoundExclusive(index);
            K r = data[index];
            if (index != --size)
            System.arraycopy(data, index + 1, data, index, size - index);
            data[size] = null;
            return r;
        }
        public int indexOf(Object e){
            for (int i = 0; i < size; i++)
            if (e.equals(data[i]))
                return i;
            return -1;
        }
        public boolean contains(Object e){
           return indexOf(e) != -1;
        }
        private void checkBoundExclusive(int index) {
            if (index >= size)
                throw new IndexOutOfBoundsException("Index: " + index + ", Size: " + size);
        }
    }
}