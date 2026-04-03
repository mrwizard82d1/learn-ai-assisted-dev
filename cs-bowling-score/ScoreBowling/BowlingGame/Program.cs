using ScoreBowling;

Console.WriteLine("Welcome to Bowling Game!");

Console.WriteLine("Score no rolls frame");
var noRollsFrame = new Frame();
noRollsFrame.Score.Match(s => Console.WriteLine($"Score: {s}"),
    () => Console.WriteLine("No score yet"));

Console.WriteLine("Score gutter frame");
var gutterFrame = new Frame();
gutterFrame.Roll(0);
gutterFrame.Roll(0);
gutterFrame.Score.Match(s => Console.WriteLine($"Score: {s}"),
    () => Console.WriteLine("Error! No score yet"));

Console.WriteLine("Score first roll only frame");
var firstRollFrame = new Frame();
firstRollFrame.Roll(7);
firstRollFrame.Score.Match(s => Console.WriteLine($"Score: {s}"),
    () => Console.WriteLine("No score yet"));

Console.WriteLine("Score open frame");
var openFrame = new Frame();
openFrame.Roll(3);
openFrame.Roll(6);
openFrame.Score.Match(s => Console.WriteLine($"Score: {s}"),
    () => Console.WriteLine("Error! No score yet"));
