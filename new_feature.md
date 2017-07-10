
### Problem statement

My team is building several applications in Django.  Bob and I are working on the inventory app while Jeff is working
a software validation app, both are apps in Django.  When I do a search in Slack, the conversation with Bob should be
more relevant than my conversation with Jeff.  Since I'm working on the same app with Bob, there should be more
conversation between me and Bob than with Jeff.  This feature makes the assumption that a search is more relevant if 
the conversation is with someone who I work with more closely.  

### Solution

The solution involves querying the data to get the top n "direct message correspondents" of a user 
for a given period of time.  The value for n and the time period is subject to fine tuning.
To start out, we'll try to find the top three users who I had the most number of conversations in the past 24 hours.

To get a count of number of direct messages between a user and all other users:

- Direct messages can have multiple participants, to start out, we'll limit to direct messages among two people.
- Since counting (and potentially storing the counts) requires resources and space, we'll limit the count between a 
user to all other users in the same organization (e.g. users who share the same org code).
- Assume conversations are stored as separate documents broken down either in fixed time intervals, or based on a
gap of time in the conversation.  Assume the document is also stored with a list of participants.  In the ETL process,
add an additional field in the document with a count of messages.

To query the top three corresponds in the last 24 hours, the order of these filters is designed to reduce the number 
of messages to be passed to the next filter:

- Filter to keep only conversations whose date is < 24 hours.
- Filter to keep conversation where the user is in the conversation's participants list
- Aggregate these conversation by the "other" participant, count the number of messages and order by the count.

The result of the query is the output of the solution.  One can then "boost" the relevance score for conversations with 
these three people in a search.

### Evaluation

Select a group of users for the test.  For a period of time (for example, one week) enable the feature, and then 
test with the feature disabled for the same period of time.  For a test user, collect the number of times the user 
clicks on the top search result with and without the feature.  This would correct for each person's tendency of 
clicking the top search results.  For example, with the feature disabled, Alice clicks on the first search result 20%
 of the time in a week.  With the feature enabled, she clicks on the first search result 25% of the time.  Do this 
 comparison for each user and for the top three results.  If there's significant increase in the click through rate for 
 the top search results across users, then it's an indication that there's a positive correlation between this feature 
 and the outcome.
 
### Next step

Adding the query would impact the response time of the search.  First, one would need to measure the impact.  
Secondly, look into how to minimize this impact.  There are a couple ways to do the query:

1) Do it at the time of the search.  The advantage is it only needs to be done for the users who does a search
     as opposed to all users.  The disadvantage is it will increase the response time of the search.
2) Calculate the count for each user once a day and store the count.  The advantage is it does not affect the
     response time of the search, the disadvantage is that it requires additional storage for the counts.  It also adds
     processing load to the system when these queries are done.
3) A combination of the two approaches.  The first time a user does a search in a day, the query is done and the 
    result is stored.

With more time and resource, one could look into the pros and cons of each of these approaches.

Fine tuning - 
Some of the numbers used here are arbitrary numbers.  For example, is selecting the top three direct message 
correspondents the optimal number?  What about top five?  How about the decision to aggregate the number of messages 
for the past 24 hours?  Instead of 24 hours, how about a week or even two weeks (as some projects have two week 
sprints).  With longer aggregation time, one could also decrease the interval to run the query as a part of the search.

