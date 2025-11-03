CREATE TABLE titanic (
    PassengerId INT PRIMARY KEY,
     Survived BOOLEAN,
     Pclass INT NOT NULL,
     Name VARCHAR(255) NOT NULL,
     Sex VARCHAR(10) CHECK (Sex IN ('male','female')),
     Age DECIMAL(3,1),
     SibSp INT,
     Parch INT,
     Ticket VARCHAR(50),
     Fare DECIMAL(8,4),
     Cabin VARCHAR(16),
     Embarked CHAR(1) CHECK (Embarked IN ('C','Q','S'))
);
INSERT INTO titanic
SELECT *
FROM CSVREAD('src/main/resources/titanic.csv', NULL, 'fieldSeparator=;');
