
# Design

On a high level, this problem is broken down into two components: parsing and analytics.

- Parsing, as its name suggests, takes the raw data and transform them into something to be consumed by analytics.
- Analytics then tries to answer the questions.

The two pieces should share a well defined interface such that in a larger project, the two components can be worked on 
in parallel by different teams and even using different programming languages.

## Design decision - the interface

To define "interface" between parsing and analytics depends what information is needed and how should it be "passed" 
between the two components.

Information needed from the email headers:
- from
- to/cc/bcc
- subject
- date
- file ID

The information needed has a well defined structure and thus it would be efficient for processing
if this information about its structure can be passed to the analytics piece.  Avro is chosen because
a schema can be defined for the data and is stored with the data, thus making it a well defined intermediate
format for parsing and analytics.  Avro data structure is also a good enough support for our data. 
For example, the "to" field needs to be a list of indefinite size, and Avro offers the array data structure.
If a SQL database is used instead, additional data manipulation would be needed (for example, to store the "to" list 
in a text field and parse it as a list during the analytics phase).

Advantages:
- Data is read and parsed only once
- Avro stores the intermediate data in a compact format allowing the parsing and analytics components to be
  even more separated (e.g. they can be run on different machines for scalability).

Disadvantage:
- Additional data storage is needed for the Avro files.

## Design decision - analytics 

Spark is a fast (compared to Hadoop MapReduce) processing engine for clustered computing, allowing the solution to be 
scaled to a larger dataset.  And since our intermediate data is structured with a schema, SparkSQL is chosen to take 
advantage of the SQL like querying power it offers.  SparkSQL also offers user defined functions which came in handy 
in the more complicated query for finding the response time.

## Design decision - choice of language

- Parsing - Avro supports different languages.  Python is chosen for its built in email parser and its ease of use in 
terms of data manipulation.
- Analytics - Scala is chosen since we use Spark for this component and Scala is the native language for Spark.

# Assumptions of the data

- Each file is treated as one email message, inner emails are not scanned.

# Runtime environment, testing, etc

The solution is tested on Spark 2.1.1 bundled with Hadoop 2.7.
