# drd-race
drd-race is a DRD public API. It contains:
- model classes: race, access, etc
- I/O helpers for serialization/deserialization in/to streams
 
### Usage
RaceIO.read() reads all races from file once, throwing exception if IO or deserialization error occurs.

```java
List<Race> races = RaceIO.read("drd_races.log");
```

RaceIO.open() opens file with races and notifies caller about new races, file deletion and re-creation. To stop notifying simply deactivate handle that is returned from this method.

```java
RaceIO.Handle handle = RaceIO.open("drd_races.log", new RaceCallback() {
    @Override
    public void newData(List<Race> list) {
        //new races
    }

    @Override
    public void deleted() {
        //file with races was deleted
    }

    @Override
    public void created() {
        //file with races was created
    }

    @Override
    public void error(RaceIOException e) {
        //error occurred
    }
});
/*...*/
//finally unsubscribe
handle.deactivate()
```