RSpec::Matchers.define :be_2xx do |expected|
  match do |res|
    (200..206).include?(res.status_code)
  end
end

RSpec::Matchers.define :be_unauthorized do |expected|
  match do |res|
    res.status_code == 401
  end
end

RSpec::Matchers.define :be_forbidden do |expected|
  match do |res|
    res.status_code == 403
  end
end

RSpec::Matchers.define :be_missing do |expected|
  match do |res|
    res.status_code == 404
  end
end

module SpecUtils
  # Copied from Minitest.  The Rspec output matcher doesn't work very will with
  # blocks that are throwing exceptions.
  def capture_io
    begin
      orig_stdout, orig_stderr         = $stdout, $stderr
      captured_stdout, captured_stderr = StringIO.new, StringIO.new
      $stdout, $stderr                 = captured_stdout, captured_stderr

      yield

      return captured_stdout.string, captured_stderr.string
    ensure
      $stdout = orig_stdout
      $stderr = orig_stderr
    end
  end
end

RSpec.configure do |config|
  config.color = true
  config.expect_with :rspec do |c|
    c.syntax = :expect
  end
  config.include SpecUtils
end


